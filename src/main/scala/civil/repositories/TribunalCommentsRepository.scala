package civil.repositories

import civil.errors.AppError
import civil.errors.AppError.InternalServerError
import civil.models._
import civil.models.enums.TribunalCommentType
import civil.repositories.QuillContextQueries.getTribunalCommentsWithReplies
import civil.utils.CommentsTreeConstructor
import io.scalaland.chimney.dsl._
import zio._

import java.util.UUID

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

case class TribunalCommentsRepositoryLive() extends TribunalCommentsRepository {

  import QuillContextHelper.ctx._

  override def insertComment(
      comment: TribunalComments
  ): ZIO[Any, AppError, TribunalCommentsReply] = {
    for {
      userReportsJoin <- ZIO
        .attempt(
          run(
            query[Users]
              .filter(u => u.userId == lift(comment.createdByUserId))
              .leftJoin(query[Reports])
              .on(_.userId == _.userId)
          ).head
        )
        .mapError(e => InternalServerError(e.toString))
      (_, report) = userReportsJoin
      userContentJoin <- ZIO
        .attempt(
          run(
            query[Users]
              .filter(u => u.userId == lift(comment.createdByUserId))
              .leftJoin(query[Topics])
              .on(_.userId == _.createdByUserId)
              .leftJoin(query[Comments])
              .on(_._1.userId == _.createdByUserId)
          ).head
        )
        .mapError(e => InternalServerError(e.toString))
      ((user, topicOpt), commentOpt) = userContentJoin
      commentType =
        if (report.isDefined)
          TribunalCommentType.Reporter
        else if (commentOpt.isDefined || topicOpt.isDefined)
          TribunalCommentType.Defendant
        else TribunalCommentType.General
      insertedComment <- ZIO
        .attempt(
          run(
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
        )
        .mapError(e => InternalServerError(e.toString))
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
    getCommentsByCommentType(userId, contentId, commentType)
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
    } yield reporterComments ++ generalComments ++ defendantComments ++ juryComments
  }

  private def getCommentsByCommentType(
      userId: String,
      contentId: UUID,
      commentType: TribunalCommentType
  ) = {
    for {
      commentsUsersJoin <- ZIO
        .attempt(
          run(
            query[TribunalComments]
              .filter(r => r.reportedContentId == lift(contentId))
              .join(query[Users])
              .on(_.createdByUserId == _.userId)
          )
        )
        .mapError(e => InternalServerError(e.toString))
      rootComments = commentsUsersJoin.filter { case (comment, _) =>
        comment.parentId.isEmpty
      }
      commentIdToUserIconSrcMap = commentsUsersJoin.foldLeft(
        Map[UUID, String]()
      ) { (m, commentUserTuple) =>
        m + (commentUserTuple._1.id -> commentUserTuple._2.iconSrc.getOrElse(
          ""
        ))
      }
      likes <- ZIO
        .attempt(
          run(query[CommentLikes].filter(cl => cl.userId == lift(userId)))
        )
        .mapError(e => InternalServerError(e.toString))
      likesMap = likes.foldLeft(Map[UUID, Int]()) { (m, t) =>
        m + (t.commentId -> t.value)
      }
      civility <- ZIO
        .attempt(
          run(
            query[CommentCivility].filter(cc => cc.userId == lift(userId))
          )
        )
        .mapError(e => InternalServerError(e.toString))
      civilityMap = civility.foldLeft(Map[UUID, Float]()) { (m, t) =>
        m + (t.commentId -> t.value)
      }
      commentsNodes = rootComments
        .map {
          case (comment, user) => {
            val commentsWithDepth =
              run(getTribunalCommentsWithReplies(lift(comment.id)))
            val repliesWithDepth = commentsWithDepth
              .map(c =>
                TribunalComments.withDepthToReplyWithDepth(
                  c,
                  likesMap.getOrElse(c.id, 0),
                  civilityMap.getOrElse(c.id, 0),
                  commentIdToUserIconSrcMap(c.id),
                  user.userId,
                  user.experience
                )
              )
              .reverse
            val replies = commentsWithDepth
              .map(c =>
                TribunalComments.commentToCommentReply(
                  c,
                  likesMap.getOrElse(c.id, 0),
                  civilityMap.getOrElse(c.id, 0),
                  commentIdToUserIconSrcMap(c.id),
                  user.userId,
                  user.experience
                )
              )

            val tc = CommentsTreeConstructor
            val replyTree =
              tc.constructTribunal(repliesWithDepth, replies).toList.head
            replyTree
          }
        }
        .filter(node => node.data.commentType == commentType)
        .sortWith((t1, t2) => t2.data.createdAt.isBefore(t1.data.createdAt))
    } yield commentsNodes
  }

}

object TribunalCommentsRepositoryLive {
  val layer: URLayer[Any, TribunalCommentsRepository] = ZLayer.fromFunction(TribunalCommentsRepositoryLive.apply _)
}


