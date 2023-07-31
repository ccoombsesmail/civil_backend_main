package civil.repositories.comments

import civil.errors.AppError
import civil.errors.AppError.{GeneralError, InternalServerError}
import civil.models._
import civil.models.actions.{LikeAction, NeutralState}
import civil.repositories.QuillContextQueries.getCommentsWithReplies
import civil.utils.CommentsTreeConstructor
import io.getquill.Ord
import io.scalaland.chimney.dsl._
import zio._

import java.util.UUID
import javax.sql.DataSource

trait CommentsRepository {
  def insertComment(
      comment: Comments,
      requestingUserData: JwtUserClaimsData
  ): ZIO[Any, AppError, CommentReply]
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
    ZIO.serviceWithZIO[CommentsRepository](
      _.insertComment(comment, requestingUserData)
    )
  def getComments(
      userId: String,
      discussionId: UUID,
      skip: Int
  ): ZIO[CommentsRepository, AppError, List[CommentNode]] =
    ZIO.serviceWithZIO[CommentsRepository](
      _.getComments(userId, discussionId, skip)
    )
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

case class CommentsRepositoryLive(dataSource: DataSource)
    extends CommentsRepository {
  import civil.repositories.QuillContext._
  override def insertComment(
      comment: Comments,
      requestingUserData: JwtUserClaimsData
  ): ZIO[Any, AppError, CommentReply] = {
    for {
      inserted <- run(
        query[Comments]
          .insertValue(lift(comment))
          .returning(c => c)
      ).mapError(e => InternalServerError(e.toString))
        .provideEnvironment(ZEnvironment(dataSource))

    } yield inserted
      .into[CommentReply]
      .withFieldConst(_.likeState, NeutralState)
      .withFieldConst(_.createdByUserId, requestingUserData.userId)
      .withFieldConst(_.createdByIconSrc, requestingUserData.userIconSrc)
      .withFieldConst(_.civility, 0.0f)
      .withFieldConst(_.createdByExperience, requestingUserData.experience)
      .withFieldConst(_.createdByTag, Some(requestingUserData.userCivilTag))
      .transform

  }

  override def getComments(
      userId: String,
      discussionId: UUID,
      skip: Int
  ): ZIO[Any, AppError, List[CommentNode]] = {

    (for {
      joinedDataQuery <- run {
        query[Comments]
          .filter(c =>
            c.discussionId == lift(discussionId) && c.parentId.isEmpty
          )
          .join(query[Users])
          .on(_.createdByUserId == _.userId)
          .leftJoin(query[CommentLikes].filter(l => l.userId == lift(userId)))
          .on(_._1.id == _.commentId)
          .leftJoin(
            query[CommentCivility].filter(l => l.userId == lift(userId))
          )
          .on(_._1._1.id == _.commentId)
          .map { case (((a, b), c), d) => (a, b, c, d) }
          .sortBy { case (comment, _, _, _) => comment.createdAt }(Ord.desc)
          .drop(lift(skip))
          .take(10)
      }
        .mapError(e => {
          InternalServerError(e.toString)
        })
      commentsWithReplies <- ZIO
        .collectAll(
          joinedDataQuery
            .map { case (comment, user, _, _) =>
              for {
                comments <- run(
                  getCommentsWithReplies(lift(comment.id), lift(userId))
                )
                repliesWithDepth = comments
                  .map(c =>
                    Comments.commentToCommentReplyWithDepth(
                      c.transformInto[CommentWithDepth],
                      c.likeState.getOrElse(NeutralState),
                      c.civility.getOrElse(0),
                      c.userIconSrc.getOrElse(""),
                      c.userId,
                      c.userExperience,
                      c.createdByTag
                    )
                  )
                  .reverse
                replies = comments
                  .map(c =>
                    Comments.commentToCommentReply(
                      c.transformInto[CommentWithDepth],
                      c.likeState.getOrElse(NeutralState),
                      c.civility.getOrElse(0),
                      c.userIconSrc.getOrElse(""),
                      c.userId,
                      c.userExperience,
                      c.createdByTag
                    )
                  )

                tc = CommentsTreeConstructor
                replyTree = tc.construct(repliesWithDepth, replies).toList.head

              } yield replyTree
            }
        )
        .mapError(e => {
          InternalServerError(e.toString)
        })
    } yield commentsWithReplies).provideEnvironment(ZEnvironment(dataSource))

  }

  override def getComment(
      userId: String,
      commentId: UUID
  ): ZIO[Any, AppError, CommentReply] = {
    (for {
      commentWithUserData <-
        run(
          query[Comments]
            .filter(c => c.id == lift(commentId))
            .join(query[Users])
            .on(_.createdByUserId == _.userId)
        ).head
      (comment, user) = commentWithUserData
//      user <- run(query[Users].filter(u => u.userId == lift(userId)))
//      userData <- ZIO
//        .fromOption(user.headOption)
    } yield comment
      .into[CommentReply]
      .withFieldConst(_.createdByIconSrc, user.iconSrc.getOrElse(""))
      .withFieldConst(_.createdByUserId, user.userId)
      .withFieldConst(_.createdByExperience, user.experience)
      .withFieldConst(_.createdByTag, user.tag)
      .withFieldConst(_.likeState, NeutralState)
      .withFieldConst(_.civility, 0f)
      .transform)
      .mapError(e => InternalServerError(e.toString))
      .provideEnvironment(ZEnvironment(dataSource))
  }

  override def getAllCommentReplies(
      userId: String,
      commentId: UUID
  ): ZIO[Any, AppError, CommentWithReplies] = {

    (for {
      commentUser <-
        run(
          query[Comments]
            .filter(c => c.id == lift(commentId))
            .join(query[Users])
            .on(_.createdByUserId == _.userId)
            .leftJoin(
              query[CommentLikes].filter(l => l.userId == lift(userId))
            )
            .on(_._1.id == _.commentId)
            .leftJoin(
              query[CommentCivility].filter(l => l.userId == lift(userId))
            )
            .on(_._1._1.id == _.commentId)
            .map { case (((a, b), c), d) => (a, b, c, d) }
        ).mapError(e => InternalServerError(e.toString))
      commentUserData <- ZIO
        .fromOption(commentUser.headOption)
        .mapError(e => InternalServerError(e.toString))

      (comment, user, likeOpt, civilityOpt) = commentUserData
      joinedData <- run {
        query[Comments]
          .filter(c => c.parentId == lift(Option(commentId)))
          .join(query[Users])
          .on(_.createdByUserId == _.userId)
          .leftJoin(query[CommentLikes].filter(l => l.userId == lift(userId)))
          .on(_._1.id == _.commentId)
          .leftJoin(
            query[CommentCivility].filter(l => l.userId == lift(userId))
          )
          .on(_._1._1.id == _.commentId)
          .map { case (((a, b), c), d) => (a, b, c, d) }
          .sortBy { case (comment, _, _, _) => comment.createdAt }(Ord.desc)
      }
        .mapError(e => InternalServerError(e.getMessage))
      commentsWithReplies <- ZIO
        .collectAll(
          joinedData
            .map { case (comment, user, likeOpt, civilityOpt) =>
              for {
                comments <- run(
                  getCommentsWithReplies(lift(comment.id), lift(userId))
                )
                repliesWithDepth = comments
                  .map(c =>
                    Comments.commentToCommentReplyWithDepth(
                      c.transformInto[CommentWithDepth],
                      c.likeState.getOrElse(NeutralState),
                      c.civility.getOrElse(0),
                      c.userIconSrc.getOrElse(""),
                      c.userId,
                      c.userExperience,
                      c.createdByTag
                    )
                  )
                  .reverse

                replies = comments
                  .map(c =>
                    Comments.commentToCommentReply(
                      c.transformInto[CommentWithDepth],
                      c.likeState.getOrElse(NeutralState),
                      c.civility.getOrElse(0),
                      c.userIconSrc.getOrElse(""),
                      c.userId,
                      c.userExperience,
                      c.createdByTag
                    )
                  )

                tc = CommentsTreeConstructor
                replyTree = tc.construct(repliesWithDepth, replies).toList.head
              } yield replyTree
            }
        )
        .mapError(e => InternalServerError(e.toString))
    } yield CommentWithReplies(
      replies = commentsWithReplies,
      comment = comment
        .into[CommentReply]
        .withFieldConst(_.createdByUserId, user.userId)
        .withFieldConst(_.createdByExperience, user.experience)
        .withFieldConst(_.createdByIconSrc, user.iconSrc.get)
        .withFieldConst(_.createdByTag, user.tag)
        .withFieldConst(
          _.likeState,
          likeOpt.map(_.likeState).getOrElse(NeutralState)
        )
        .withFieldConst(_.civility, civilityOpt.map(_.value).getOrElse(0f))
        .transform
    )).provideEnvironment(ZEnvironment(dataSource))

  }

  override def getUserComments(
      requestingUserId: String,
      userId: String
  ): ZIO[Any, AppError, List[CommentNode]] = {

    (for {
      commentsUsersJoin <-
        run(
          query[Comments]
            .filter(c => c.createdByUserId == lift(userId))
            .join(query[Users])
            .on(_.createdByUserId == _.userId)
        )

      rootComments = commentsUsersJoin.filter(j => j._1.parentId.isEmpty)

      commentsWithReplies <- ZIO
        .collectAll(
          rootComments
            .map(joined => {
              val c = joined._1

              for {
                comments <- run(
                  getCommentsWithReplies(lift(c.id), lift(userId))
                )
                repliesWithDepth = comments
                  .map(c =>
                    Comments.commentToCommentReplyWithDepth(
                      c.transformInto[CommentWithDepth],
                      c.likeState.getOrElse(NeutralState),
                      c.civility.getOrElse(0),
                      c.userIconSrc.getOrElse(""),
                      c.userId,
                      c.userExperience,
                      c.createdByTag
                    )
                  )
                  .reverse
                replies = comments
                  .map(c =>
                    Comments.commentToCommentReply(
                      c.transformInto[CommentWithDepth],
                      c.likeState.getOrElse(NeutralState),
                      c.civility.getOrElse(0),
                      c.userIconSrc.getOrElse(""),
                      c.userId,
                      c.userExperience,
                      c.createdByTag
                    )
                  )

                tc = CommentsTreeConstructor
                replyTree = tc.construct(repliesWithDepth, replies).toList.head
              } yield replyTree
            })
        )

    } yield commentsWithReplies)
      .mapError(e => InternalServerError(e.getMessage))
      .provideEnvironment(ZEnvironment(dataSource))

  }
}

object CommentsRepositoryLive {
  val layer: URLayer[DataSource, CommentsRepository] =
    ZLayer.fromFunction(CommentsRepositoryLive.apply _)

}
