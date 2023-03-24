package civil.repositories

import civil.models.{CommentCivility, CommentLikes, Comments, ErrorInfo, InternalServerError, Reports, Topics, TribunalCommentNode, TribunalComments, TribunalCommentsReply, Users}
import civil.models.enums.TribunalCommentType
import civil.models._
import civil.repositories.QuillContextQueries.getTribunalCommentsWithReplies
import civil.utils.CommentsTreeConstructor
import io.scalaland.chimney.dsl._
import zio._

import java.util.UUID

trait TribunalCommentsRepository {
  def insertComment(
      comment: TribunalComments
  ): ZIO[Any, ErrorInfo, TribunalCommentsReply]

  def getComments(
      userId: String,
      contentId: UUID,
      commentType: TribunalCommentType
  ): ZIO[Any, ErrorInfo, List[TribunalCommentNode]]

  def getCommentsBatch(
      userId: String,
      contentId: UUID
  ): ZIO[Any, ErrorInfo, List[TribunalCommentNode]]

}

object TribunalCommentsRepository {
  def insertComment(
      comment: TribunalComments
  ): ZIO[Has[TribunalCommentsRepository], ErrorInfo, TribunalCommentsReply] =
    ZIO.serviceWith[TribunalCommentsRepository](_.insertComment(comment))

  def getComments(
      userId: String,
      contentId: UUID,
      commentType: TribunalCommentType
  ): ZIO[Has[TribunalCommentsRepository], ErrorInfo, List[
    TribunalCommentNode
  ]] =
    ZIO.serviceWith[TribunalCommentsRepository](
      _.getComments(userId, contentId, commentType)
    )

  def getCommentsBatch(
      userId: String,
      contentId: UUID
  ): ZIO[Has[
    TribunalCommentsRepository
  ], ErrorInfo, List[TribunalCommentNode]] =
    ZIO.serviceWith[TribunalCommentsRepository](
      _.getCommentsBatch(userId, contentId)
    )

}

case class TribunalCommentsRepositoryLive() extends TribunalCommentsRepository {

  import QuillContextHelper.ctx._

  override def insertComment(
      comment: TribunalComments
  ): ZIO[Any, ErrorInfo, TribunalCommentsReply] = {
    for {
      userReportsJoin <- ZIO
        .effect(
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
        .effect(
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
        .effect(
          run(
            query[TribunalComments]
              .insert(
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
  ): ZIO[Any, ErrorInfo, List[TribunalCommentNode]] = {
    getCommentsByCommentType(userId, contentId, commentType)
  }

  override def getCommentsBatch(
      userId: String,
      contentId: UUID
  ): ZIO[Any, ErrorInfo, List[TribunalCommentNode]] = {
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
        .effect(
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
        .effect(
          run(query[CommentLikes].filter(cl => cl.userId == lift(userId)))
        )
        .mapError(e => InternalServerError(e.toString))
      likesMap = likes.foldLeft(Map[UUID, Int]()) { (m, t) =>
        m + (t.commentId -> t.value)
      }
      civility <- ZIO
        .effect(
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
  val live: ZLayer[Any, Throwable, Has[TribunalCommentsRepository]] =
    ZLayer.succeed(TribunalCommentsRepositoryLive())
}

////****** if want to change to a join later ******/////
//commentsLikesCivilityJoin <- ZIO
//.effect(
//run(
//query[TribunalComments]
//.filter(r => r.reportedContentId == lift(contentId))
//.leftJoin(query[CommentLikes])
//.on((tc, cl) => tc.id == cl.commentId).filter { case (_, like) => like.exists(_.userId == lift(userId)) }
//  .leftJoin(query[CommentCivility])
//  .on { case ((tc, _), cc) =>
//  tc.userId == cc.userId && tc.id == cc.commentId
//}
//  )
//  )
//  .mapError(e => InternalServerError(e.toString))

//likesMap = commentsLikesCivilityJoin.foldLeft(Map[UUID, Int]()) {
//  (m, c) =>
//{
//  val commentLike = c._1
//  if (commentLike._2.isDefined)
//  m + (commentLike._1.id -> commentLike._2.get.value)
//  else m
//}
