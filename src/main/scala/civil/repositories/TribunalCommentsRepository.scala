package civil.repositories

import civil.errors.AppError
import civil.errors.AppError.InternalServerError
import civil.models._
import civil.models.actions.NeutralState
import civil.models.enums.TribunalCommentType
import civil.repositories.QuillContextQueries.getTribunalCommentsWithReplies
import civil.utils.CommentsTreeConstructor
import io.getquill.Ord
import io.scalaland.chimney.dsl._
import zio._

import java.util.UUID
import javax.sql.DataSource

trait TribunalCommentsRepository {
  def insertComment(
      comment: TribunalComments,
      commentCreatorData: JwtUserClaimsData
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
      comment: TribunalComments,
      commentCreatorData: JwtUserClaimsData

                   ): ZIO[TribunalCommentsRepository, AppError, TribunalCommentsReply] =
    ZIO.serviceWithZIO[TribunalCommentsRepository](_.insertComment(comment, commentCreatorData))

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

case class TribunalCommentsRepositoryLive(dataSource: DataSource)
    extends TribunalCommentsRepository {

  import civil.repositories.QuillContext._

  override def insertComment(
      comment: TribunalComments,
      commentCreatorData: JwtUserClaimsData

  ): ZIO[Any, AppError, TribunalCommentsReply] = {
    (for {
      userReport <- run(
        query[Reports]
          .filter(r => r.userId == lift(comment.createdByUserId) && r.contentId == lift(comment.reportedContentId))
      )
      _ = println(userReport)
      isReporter = userReport.nonEmpty
      _ = println(isReporter)

      userContentJoin <- run(
        query[Users]
          .leftJoin(query[Spaces]).on((u, t) => u.userId == t.createdByUserId)
          .leftJoin(query[Comments]).on((ut, c) => ut._1.userId == c.createdByUserId)
          .filter {
            case ((u, tOpt), cOpt) =>
              u.userId == lift(comment.createdByUserId) &&
                (
                  tOpt.exists(_.id == lift(comment.reportedContentId)) ||
                    cOpt.exists(_.id == lift(comment.reportedContentId))
                  )
          }
          .map { case ((u, tOpt), cOpt) => (u, tOpt, cOpt) } // Select only required fields if needed
      ).mapError(e => {
        println(e)
        e
      })

      (user, topicOpt, commentOpt) = userContentJoin.headOption.getOrElse(User(
        commentCreatorData.userId,
        Some(commentCreatorData.userIconSrc),
        Some(commentCreatorData.userCivilTag),
        commentCreatorData.username,
        commentCreatorData.experience
      ), None, None)
      _ = println("commentOpt", commentOpt)

      commentType =
        if (commentOpt.isDefined || topicOpt.isDefined)
          TribunalCommentType.Defendant
        else if (isReporter)
          TribunalCommentType.Reporter
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
    } yield insertedComment
      .into[TribunalCommentsReply]
      .withFieldConst(_.createdByExperience, commentCreatorData.experience)
      .withFieldConst(_.createdByIconSrc, commentCreatorData.userIconSrc)
      .withFieldConst(_.createdByUserId, commentCreatorData.userId)
      .withFieldConst(_.createdByTag, Some(commentCreatorData.userCivilTag))
      .withFieldConst(_.likeState, NeutralState)
      .withFieldConst(_.civility, 0f)
      .transform).mapError(e => InternalServerError(e.toString)).provideEnvironment(ZEnvironment(dataSource))

  }

  override def getComments(
      userId: String,
      contentId: UUID,
      commentType: TribunalCommentType
  ): ZIO[Any, AppError, List[TribunalCommentNode]] = {
    if (commentType == TribunalCommentType.All)
      getAllComments(userId, contentId)
    else
      getCommentsByCommentType(userId, contentId, commentType)
  }

  override def getCommentsBatch(
      userId: String,
      contentId: UUID
  ): ZIO[Any, AppError, List[TribunalCommentNode]] = {
    (for {
      reporterComments <- getCommentsByCommentType(
        userId,
        contentId,
        TribunalCommentType.Reporter
      )
      generalComments <- getCommentsByCommentType(
        userId,
        contentId,
        TribunalCommentType.General
      )
      defendantComments <- getCommentsByCommentType(
        userId,
        contentId,
        TribunalCommentType.Defendant
      )
      juryComments <- getCommentsByCommentType(
        userId,
        contentId,
        TribunalCommentType.Jury
      )
    } yield reporterComments ++ generalComments ++ defendantComments ++ juryComments)
      .provideEnvironment(ZEnvironment(dataSource))
  }

  private def getAllComments(
      userId: String,
      contentId: UUID
  ): ZIO[Any, InternalServerError, List[TribunalCommentNode]] = (for {
    commentsWithUserLikesCivility <- run(
      query[TribunalComments]
        .filter(c =>
          c.reportedContentId == lift(contentId) && c.parentId.isEmpty
        )
        .join(query[Users])
        .on(_.createdByUserId == _.userId)
        .leftJoin(query[CommentLikes].filter(_.userId == lift(userId)))
        .on(_._1.id == _.commentId)
        .leftJoin(query[CommentCivility].filter(_.userId == lift(userId)))
        .on(_._1._1.id == _.commentId)
        .map { case (((a, b), c), d) => (a, b, c, d) }
        .sortBy { case (comment, _, _, _) => comment.createdAt }(Ord.desc)
    ).mapError(e => {
      println(e)
      InternalServerError(e.toString)
    })
    commentsNodes <- ZIO
      .collectAll(commentsWithUserLikesCivility.map {
        case (comment, user, _, _) =>
          for {
            commentsWithDepth <- run(
              getTribunalCommentsWithReplies(lift(comment.id), lift(userId))
            ).mapError(e => {
              println(e)
              e
            })
            repliesWithDepth = commentsWithDepth.map { c =>
              TribunalComments.withDepthToReplyWithDepth(
                c,
                c.likeState.getOrElse(NeutralState),
                c.civility.getOrElse(0f),
                c.userIconSrc.getOrElse(""),
                c.userId,
                c.userExperience,
                c.createdByTag
              )
            }.reverse

            replies = commentsWithDepth.map { c =>
              TribunalComments.commentToCommentReply(
                c,
                c.likeState.getOrElse(NeutralState),
                c.civility.getOrElse(0f),
                c.userIconSrc.getOrElse(""),
                c.userId,
                c.userExperience,
                c.createdByTag
              )
            }
            tc = CommentsTreeConstructor
            replyTree <- ZIO.fromOption(tc
              .constructTribunal(repliesWithDepth, replies)
              .headOption).mapError(e => {
                println(e.toString)
                e
              })
          } yield replyTree
      })
  } yield commentsNodes)
    .provideEnvironment(ZEnvironment(dataSource))
    .mapError(e => {
      println(e.toString)
      InternalServerError(e.toString)
    })
  private def getCommentsByCommentType(
      userId: String,
      contentId: UUID,
      commentType: TribunalCommentType
  ): ZIO[Any, InternalServerError, List[TribunalCommentNode]] = (for {
    commentsWithUserLikesCivility <- run(
      query[TribunalComments]
        .filter(c =>
          c.reportedContentId == lift(contentId) && c.parentId.isEmpty
        )
        .filter(c => c.commentType == lift(commentType))
        .join(query[Users])
        .on(_.createdByUserId == _.userId)
        .leftJoin(query[CommentLikes].filter(_.userId == lift(userId)))
        .on(_._1.id == _.commentId)
        .leftJoin(query[CommentCivility].filter(_.userId == lift(userId)))
        .on(_._1._1.id == _.commentId)
    )
    commentsNodes <- ZIO
      .collectAll(commentsWithUserLikesCivility.map {
        case (((comment, user), _), _) =>

          for {
            commentsWithDepth <- run(
              getTribunalCommentsWithReplies(lift(comment.id), lift(userId))
            )
            repliesWithDepth = commentsWithDepth.map { c =>
              TribunalComments.withDepthToReplyWithDepth(
                c,
                c.likeState.getOrElse(NeutralState),
                c.civility.getOrElse(0),
                c.userIconSrc.getOrElse(""),
                c.userId,
                c.userExperience,
                c.createdByTag
              )
            }.reverse
            replies = commentsWithDepth.map { c =>
              TribunalComments.commentToCommentReply(
                c,
                c.likeState.getOrElse(NeutralState),
                c.civility.getOrElse(0),
                c.userIconSrc.getOrElse(""),
                c.userId,
                c.userExperience,
                c.createdByTag
              )
            }
            tc = CommentsTreeConstructor
            replyTree = tc
              .constructTribunal(repliesWithDepth, replies)
              .toList
              .head
          } yield replyTree
      })
  } yield commentsNodes)
    .provideEnvironment(ZEnvironment(dataSource))
    .mapError(e => InternalServerError(e.toString))

}

object TribunalCommentsRepositoryLive {
  val layer: URLayer[DataSource, TribunalCommentsRepository] =
    ZLayer.fromFunction(TribunalCommentsRepositoryLive.apply _)
}
