package civil.repositories

import civil.errors.AppError
import civil.errors.AppError.InternalServerError
import civil.models._
import civil.models.enums.TribunalCommentType
import civil.repositories.QuillContextQueries.getTribunalCommentsWithReplies
import civil.utils.CommentsTreeConstructor
import io.scalaland.chimney.dsl._
import zio._

import java.sql.SQLException
import java.util.UUID
import javax.sql.DataSource

trait TribunalCommentsRepository {
  def insertComment(
      comment: TribunalComments
  ): ZIO[Any, AppError, TribunalCommentsReply]

  def getComments(
      userId: String,
      contentId: UUID,
      commentType: TribunalCommentType
  ): ZIO[Any, AppError, List[TribunalCommentNode]]

  def getCommentsBatch(
      userId: String,
      contentId: UUID
  ): ZIO[Any, AppError, List[TribunalCommentNode]]

}

object TribunalCommentsRepository {
  def insertComment(
      comment: TribunalComments
  ): ZIO[TribunalCommentsRepository, AppError, TribunalCommentsReply] =
    ZIO.serviceWithZIO[TribunalCommentsRepository](_.insertComment(comment))

  def getComments(
      userId: String,
      contentId: UUID,
      commentType: TribunalCommentType
  ): ZIO[TribunalCommentsRepository, AppError, List[
    TribunalCommentNode
  ]] =
    ZIO.serviceWithZIO[TribunalCommentsRepository](
      _.getComments(userId, contentId, commentType)
    )

  def getCommentsBatch(
      userId: String,
      contentId: UUID
  ): ZIO[TribunalCommentsRepository, AppError, List[TribunalCommentNode]] =
    ZIO.serviceWithZIO[TribunalCommentsRepository](
      _.getCommentsBatch(userId, contentId)
    )

}

case class TribunalCommentsRepositoryLive(dataSource: DataSource) extends TribunalCommentsRepository {

  import civil.repositories.QuillContext._

  override def insertComment(
      comment: TribunalComments
  ): ZIO[Any, AppError, TribunalCommentsReply] = {
    for {
      userReportsJoin <- run(
            query[Users]
              .filter(u => u.userId == lift(comment.createdByUserId))
              .leftJoin(query[Reports])
              .on(_.userId == _.userId)
          )
        .mapError(e => InternalServerError(e.toString))
        .provideEnvironment(ZEnvironment(dataSource))
      (_, report) = userReportsJoin.head
      userContentJoin <- run(
            query[Users]
              .filter(u => u.userId == lift(comment.createdByUserId))
              .leftJoin(query[Topics])
              .on(_.userId == _.createdByUserId)
              .leftJoin(query[Comments])
              .on(_._1.userId == _.createdByUserId)
        )
        .mapError(e => InternalServerError(e.toString))
        .provideEnvironment(ZEnvironment(dataSource))
      ((user, topicOpt), commentOpt) = userContentJoin.head
      commentType =
        if (report.isDefined)
          TribunalCommentType.Reporter
        else if (commentOpt.isDefined || topicOpt.isDefined)
          TribunalCommentType.Defendant
        else TribunalCommentType.General
      insertedComment <- run(
            query[TribunalComments]
              .insertValue(
                lift(
                  comment.copy(
                    commentType = commentType
                  )
                )
              )
              .returning(c => c)
        )
        .mapError(e => InternalServerError(e.toString)).provideEnvironment(ZEnvironment(dataSource))
    } yield insertedComment
      .into[TribunalCommentsReply]
      .withFieldConst(_.createdByExperience, user.experience)
      .withFieldConst(_.createdByIconSrc, user.iconSrc.getOrElse(""))
      .withFieldConst(_.createdByUserId, user.userId)
      .withFieldConst(_.likeState, 0)
      .withFieldConst(_.civility, 0f)
      .transform

  }

  override def getComments(
      userId: String,
      contentId: UUID,
      commentType: TribunalCommentType
  ): ZIO[Any, AppError, List[TribunalCommentNode]] = {
    getCommentsByCommentType(userId, contentId, commentType).provideEnvironment(ZEnvironment(dataSource))
  }

  override def getCommentsBatch(
      userId: String,
      contentId: UUID
  ): ZIO[Any, AppError, List[TribunalCommentNode]] = {
    for {
      reporterComments <- getCommentsByCommentType(
        userId,
        contentId,
        TribunalCommentType.Reporter
      ).provideEnvironment(ZEnvironment(dataSource))
      generalComments <- getCommentsByCommentType(
        userId,
        contentId,
        TribunalCommentType.General
      ).provideEnvironment(ZEnvironment(dataSource))
      defendantComments <- getCommentsByCommentType(
        userId,
        contentId,
        TribunalCommentType.Defendant
      ).provideEnvironment(ZEnvironment(dataSource))
      juryComments <- getCommentsByCommentType(
        userId,
        contentId,
        TribunalCommentType.Jury
      ).provideEnvironment(ZEnvironment(dataSource))
    } yield reporterComments ++ generalComments ++ defendantComments ++ juryComments
  }

  private def getCommentsByCommentType(
      userId: String,
      contentId: UUID,
      commentType: TribunalCommentType
  ): ZIO[Any, InternalServerError, List[TribunalCommentNode]] = (for {
    commentsWithUserLikesCivility <- run(
        query[TribunalComments]
          .filter(c => c.reportedContentId == lift(contentId) && c.parentId.isEmpty)
          .filter(_.commentType == lift(commentType))
          .join(query[Users])
          .on(_.createdByUserId == _.userId)
          .leftJoin(query[CommentLikes].filter(_.userId == lift(userId)))
          .on(_._1.id == _.commentId)
          .leftJoin(query[CommentCivility].filter(_.userId == lift(userId)))
          .on(_._1._1.id == _.commentId)
    ).mapError(e => InternalServerError(e.toString)).provideEnvironment(ZEnvironment(dataSource))


    commentsNodes <- ZIO.collectAll(commentsWithUserLikesCivility.map {
        case (((comment, user), maybeLike), maybeCivility) =>
          val likes = maybeLike.map(_.value).getOrElse(0)
          val civility = maybeCivility.map(_.value).getOrElse(0f)

          for {
            commentsWithDepth <- run(getTribunalCommentsWithReplies(lift(comment.id)))
            repliesWithDepth = commentsWithDepth.map { c =>
              TribunalComments.withDepthToReplyWithDepth(
                c,
                likes,
                civility,
                c.userIconSrc.getOrElse(""),
                c.userId,
                c.userExperience
              )
            }.reverse
            replies = commentsWithDepth.map { c =>
              TribunalComments.commentToCommentReply(
                c,
                likes,
                civility,
                user.iconSrc.getOrElse(""),
                user.userId,
                user.experience
              )
            }
            tc = CommentsTreeConstructor
            replyTree = tc.constructTribunal(repliesWithDepth, replies).toList.head
          } yield replyTree
      }).provideEnvironment(ZEnvironment(dataSource)).mapError(e => InternalServerError(e.toString))
    } yield commentsNodes).provideEnvironment(ZEnvironment(dataSource)).mapError(e => InternalServerError(e.toString))

}

object TribunalCommentsRepositoryLive {
  val layer: URLayer[DataSource, TribunalCommentsRepository] = ZLayer.fromFunction(TribunalCommentsRepositoryLive.apply _)
}


