package civil.repositories.comments

import civil.errors.AppError
import civil.errors.AppError.{GeneralError, InternalServerError}
import civil.models._
import civil.repositories.topics.DiscussionRepository
import civil.repositories.{QuillContextHelper, QuillContextQueries}
import civil.utils.CommentsTreeConstructor
import io.getquill.Ord
import io.scalaland.chimney.dsl._
import zio._

import java.util.UUID

trait CommentsRepository {
  def insertComment(comment: Comments, requestingUserData: JwtUserClaimsData): ZIO[Any, AppError, CommentReply]
  def getComments(
      userId: String,
      discussionId: UUID,
      skip: Int
  ): ZIO[Any, AppError, List[CommentNode]]
  def getComment(
      userId: String,
      commentId: UUID
  ): ZIO[Any, AppError, CommentReply]

  def getAllCommentReplies(
      userId: String,
      commentId: UUID
  ): ZIO[Any, AppError, CommentWithReplies]

  def getUserComments(
       requestingUserId: String,
       userId: String
  ): ZIO[Any, AppError, List[CommentNode]]
}

object CommentsRepository {
  def insertComment(
      comment: Comments,
      requestingUserData: JwtUserClaimsData
  ): ZIO[CommentsRepository, AppError, CommentReply] =
    ZIO.serviceWithZIO[CommentsRepository](_.insertComment(comment, requestingUserData))
  def getComments(
      userId: String,
      discussionId: UUID,
      skip: Int
                 ): ZIO[CommentsRepository, AppError, List[CommentNode]] =
    ZIO.serviceWithZIO[CommentsRepository](_.getComments(userId, discussionId, skip))
  def getComment(
      userId: String,
      commentId: UUID
  ): ZIO[CommentsRepository, AppError, CommentReply] =
    ZIO.serviceWithZIO[CommentsRepository](_.getComment(userId, commentId))
  def getAllCommentReplies(
      userId: String,
      commentId: UUID
  ): ZIO[CommentsRepository, AppError, CommentWithReplies] =
    ZIO.serviceWithZIO[CommentsRepository](
      _.getAllCommentReplies(userId, commentId)
    )

  def getUserComments(
                     requestingUserId: String,
                     userId: String
                   ): ZIO[CommentsRepository, AppError, List[CommentNode]] =
    ZIO.serviceWithZIO[CommentsRepository](
      _.getUserComments(requestingUserId, userId)
    )
}

case class CommentsRepositoryLive() extends CommentsRepository {
  import QuillContextHelper.ctx._
  import QuillContextQueries.getCommentsWithReplies

  override def insertComment(
      comment: Comments,
      requestingUserData: JwtUserClaimsData
  ): ZIO[Any, AppError, CommentReply] = {
    for {
      inserted <- ZIO.attempt(run(
        query[Comments]
          .insertValue(lift(comment))
          .returning(c => c)
      )).mapError(e => InternalServerError(e.toString()))

    } yield inserted
      .into[CommentReply]
      .withFieldConst(_.likeState, 0)
      .withFieldConst(_.createdByUserId, requestingUserData.userId)
      .withFieldConst(_.createdByIconSrc, requestingUserData.userIconSrc)
      .withFieldConst(_.civility, 0.0f)
      .withFieldConst(_.createdByExperience, requestingUserData.experience)
      .transform

  }

  override def getComments(
      userId: String,
      discussionId: UUID,
      skip: Int
  ): ZIO[Any, AppError, List[CommentNode]] = {

    for {
      joinedData <- ZIO.attempt(run {
        query[Comments]
          .filter(c => c.discussionId == lift(discussionId) && c.parentId.isEmpty)
          .join(query[Users])
          .on(_.createdByUserId == _.userId)
          .leftJoin(query[CommentLikes].filter(l => l.userId == lift(userId)))
          .on(_._1.id == _.commentId)
          .leftJoin(query[CommentCivility].filter(l => l.userId == lift(userId)))
          .on(_._1._1.id == _.commentId)
          .map { case (((a, b), c), d) => (a, b, c, d) }
          .sortBy { case (comment, _, _, _) => comment.createdAt}(Ord.desc)
          .drop(lift(skip))
          .take(10)
      }).mapError(e => InternalServerError(e.getMessage))

      commentsWithReplies = joinedData
        .map { case (comment, user, likeOpt, civilityOpt) =>
          val comments = run(getCommentsWithReplies(lift(comment.id)))
          println(user)
          val repliesWithDepth: Seq[EntryWithDepth] = comments
            .map(c =>
              Comments.commentToCommentReplyWithDepth(
                c.transformInto[CommentWithDepth],
                likeOpt.map(_.value).getOrElse(0),
                civilityOpt.map(_.value).getOrElse(0),
                c.userIconSrc.getOrElse(""),
                c.userId,
                c.userExperience
              )
            )
            .reverse

          val replies = comments
            .map(c =>
              Comments.commentToCommentReply(
                c.transformInto[CommentWithDepth],
                likeOpt.map(_.value).getOrElse(0),
                civilityOpt.map(_.value).getOrElse(0),
                c.userIconSrc.getOrElse(""),
                c.userId,
                c.userExperience
              )
            )

          val tc = CommentsTreeConstructor
          val replyTree = tc.construct(repliesWithDepth, replies).toList.head
          replyTree
        }
    } yield commentsWithReplies

  }

  override def getComment(
      userId: String,
      commentId: UUID
  ): ZIO[Any, AppError, CommentReply] = {
    for {
      comment <- ZIO
        .attempt(
          run(
            query[Comments].filter(c => c.id == lift(commentId))
          ).head
        )
        .mapError(e => InternalServerError(e.toString))
      user <- ZIO
        .attempt(run(query[Users].filter(u => u.userId == lift(userId))).head)
        .mapError(e => InternalServerError(e.toString))
    } yield comment
      .into[CommentReply]
      .withFieldConst(_.createdByIconSrc, user.iconSrc.getOrElse(""))
      .withFieldConst(_.createdByUserId, user.userId)
      .withFieldConst(_.createdByExperience, user.experience)
      .withFieldConst(_.likeState, 0)
      .withFieldConst(_.civility, 0f)
      .transform
  }

  override def getAllCommentReplies(
      userId: String,
      commentId: UUID
  ): ZIO[Any, AppError, CommentWithReplies] = {

    for {
      commentUser <- ZIO
        .attempt(
          run(
            query[Comments]
              .filter(c => c.id == lift(commentId))
              .join(query[Users])
              .on(_.createdByUserId == _.userId)
              .leftJoin(query[CommentLikes].filter(l => l.userId == lift(userId)))
              .on(_._1.id == _.commentId)
              .leftJoin(query[CommentCivility].filter(l => l.userId == lift(userId)))
              .on(_._1._1.id == _.commentId)
              .map { case (((a, b), c), d) => (a, b, c, d) }
          ).head
        )
        .mapError(e => InternalServerError(e.toString))
      (comment, user, likeOpt, civilityOpt) = commentUser
      joinedData
        <- ZIO.attempt(run {
        query[Comments]
          .filter(c => c.parentId == lift(Option(commentId)))
          .join(query[Users])
          .on(_.createdByUserId == _.userId)
          .leftJoin(query[CommentLikes].filter(l => l.userId == lift(userId)))
          .on(_._1.id == _.commentId)
          .leftJoin(query[CommentCivility].filter(l => l.userId == lift(userId)))
          .on(_._1._1.id == _.commentId)
          .map { case (((a, b), c), d) => (a, b, c, d) }
          .sortBy { case (comment, _, _, _) => comment.createdAt }(Ord.desc)
      }).mapError(e => InternalServerError(e.getMessage))

      commentsWithReplies = joinedData.map { case (comment, user, likeOpt, civilityOpt) => {
          val comments = run(getCommentsWithReplies(lift(comment.id)))

          val repliesWithDepth = comments
            .map(c =>
              Comments.commentToCommentReplyWithDepth(
                c.transformInto[CommentWithDepth],
                likeOpt.map(_.value).getOrElse(0),
                civilityOpt.map(_.value).getOrElse(0f),
                c.userIconSrc.getOrElse(""),
                user.userId,
                user.experience
              )
            )
            .reverse
          val replies = comments
            .map(c =>
              Comments.commentToCommentReply(
                c.transformInto[CommentWithDepth],
                likeOpt.map(_.value).getOrElse(0),
                civilityOpt.map(_.value).getOrElse(0f),
                c.userIconSrc.getOrElse(""),
                user.userId,
                user.experience
              )
            )

          val tc = CommentsTreeConstructor
          val replyTree = tc.construct(repliesWithDepth, replies).toList.head
          replyTree
        }}
    } yield CommentWithReplies(
      replies = commentsWithReplies,
      comment = comment
        .into[CommentReply]
        .withFieldConst(_.createdByUserId, user.userId)
        .withFieldConst(_.createdByExperience, user.experience)
        .withFieldConst(_.createdByIconSrc, user.iconSrc.get)
        .withFieldConst(_.likeState, likeOpt.map(_.value).getOrElse(0))
        .withFieldConst(_.civility, civilityOpt.map(_.value).getOrElse(0f))
        .transform
    )

//    for {
//      commentUser <- ZIO
//        .effect(
//          run(
//            query[Comments]
//              .filter(c => c.id == lift(commentId))
//              .join(query[Users])
//              .on(_.createdByUserId == _.userId)
//          ).head
//        )
//        .mapError(e => InternalServerError(e.toString))
//      (comment, user) = commentUser
//      commentsUsersJoin <- ZIO
//        .effect(
//          run(
//            query[Comments]
//              .filter(c => c.parentId == lift(Option(commentId)))
//              .join(query[Users])
//              .on(_.createdByUserId == _.userId)
//          )
//        )
//        .mapError(e => InternalServerError(e.toString))
//
//      commentToUserSrcMap = commentsUsersJoin.foldLeft(Map[UUID, String]()) {
//        (m, t) =>
//          m + (t._1.id -> t._2.iconSrc.getOrElse(""))
//      }
//      likes <- ZIO
//        .effect(run(query[CommentLikes].filter(l => l.userId == lift(userId))))
//        .mapError(e => InternalServerError(e.toString))
//
//      likesMap = likes.foldLeft(Map[UUID, Int]()) { (m, t) =>
//        m + (t.commentId -> t.value)
//      }
//      civility <- ZIO
//        .effect(
//          run(
//            query[CommentCivility].filter(l => l.userId == lift(userId))
//          )
//        )
//        .mapError(e => InternalServerError(e.toString))
//
//      civilityMap = civility.foldLeft(Map[UUID, Float]()) { (m, t) =>
//        m + (t.commentId -> t.value)
//      }
//      commentsWithReplies = commentsUsersJoin
//        .map(joined => {
//          val c = joined._1
//          val u = joined._2
//          val comments = run(getCommentsWithReplies(lift(c.id)))
//
//          val repliesWithDepth = comments
//            .map(c =>
//              Comments.commentToCommentReplyWithDepth(
//                c.transformInto[CommentWithDepth],
//                likesMap.getOrElse(c.id, 0),
//                civilityMap.getOrElse(c.id, 0),
//                commentToUserSrcMap.getOrElse(c.id, ""),
//                u.userId,
//                u.experience
//              )
//            )
//            .reverse
//          val replies = comments
//            .map(c =>
//              Comments.commentToCommentReply(
//                c.transformInto[CommentWithDepth],
//                likesMap.getOrElse(c.id, 0),
//                civilityMap.getOrElse(c.id, 0),
//                commentToUserSrcMap.getOrElse(c.id, ""),
//                u.userId,
//                u.experience
//              )
//            )
//
//          val tc = CommentsTreeConstructor
//          val replyTree = tc.construct(repliesWithDepth, replies).toList.head
//          replyTree
//        })
//    } yield CommentWithReplies(
//      replies = commentsWithReplies,
//      comment = comment
//        .into[CommentReply]
//        .withFieldConst(_.createdByUserId, user.userId)
//        .withFieldConst(_.createdByExperience, user.experience)
//        .withFieldConst(_.createdByIconSrc, user.iconSrc.get)
//        .withFieldConst(_.likeState, likesMap.getOrElse(commentId, 0))
//        .withFieldConst(_.civility, civilityMap.getOrElse(commentId, 0f))
//        .transform
//    )

  }

  override def getUserComments(requestingUserId: String, userId: String): ZIO[Any, AppError, List[CommentNode]] = {


    for {
      commentsUsersJoin <- ZIO.attempt(run(
        query[Comments]
          .filter(c => c.createdByUserId == lift(userId))
          .join(query[Users])
          .on(_.createdByUserId == _.userId)
      )).mapError(e => InternalServerError(e.getMessage))
      rootComments = commentsUsersJoin.filter(j => j._1.parentId.isEmpty)
      commentToUserSrcMap = commentsUsersJoin.foldLeft(Map[UUID, String]()) {
        (m, t) =>
          m + (t._1.id -> t._2.iconSrc.getOrElse(""))
      }
      likes <- ZIO.attempt(run(query[CommentLikes].filter(l => l.userId == lift(userId)))).mapError(e => InternalServerError(e.getMessage))
      likesMap = likes.foldLeft(Map[UUID, Int]()) { (m, t) =>
        m + (t.commentId -> t.value)
      }
      civility <- ZIO.attempt(run(
        query[CommentCivility].filter(l => l.userId == lift(userId))
      )).mapError(e => InternalServerError(e.getMessage))
      civilityMap = civility.foldLeft(Map[UUID, Float]()) { (m, t) =>
        m + (t.commentId -> t.value)
      }
      commentsWithReplies = rootComments.map(joined => {
        val c = joined._1
        val u = joined._2
        val comments = run(getCommentsWithReplies(lift(c.id)))

        val positiveCivility = civilityMap.getOrElse(c.id, None)

        val repliesWithDepth = comments
          .map(c =>
            Comments.commentToCommentReplyWithDepth(
              c.transformInto[CommentWithDepth],
              likesMap.getOrElse(c.id, 0),
              civilityMap.getOrElse(c.id, 0),
              commentToUserSrcMap.getOrElse(c.id, ""),
              u.userId,
              u.experience
            )
          )
          .reverse
        val replies = comments
          .map(c =>
            Comments.commentToCommentReply(
              c.transformInto[CommentWithDepth],
              likesMap.getOrElse(c.id, 0),
              civilityMap.getOrElse(c.id, 0),
              commentToUserSrcMap.getOrElse(c.id, ""),
              u.userId,
              u.experience
            )
          )

        val tc = CommentsTreeConstructor
        val replyTree = tc.construct(repliesWithDepth, replies).toList.head
        replyTree
      })
        .sortWith((t1, t2) => t2.data.createdAt.isBefore(t1.data.createdAt))
    } yield commentsWithReplies

  }
}

object CommentsRepositoryLive {
  val layer: URLayer[Any, CommentsRepository] = ZLayer.fromFunction(CommentsRepositoryLive.apply _)

}

