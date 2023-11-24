package civil.repositories.comments

import cats.implicits.catsSyntaxOptionId
import civil.errors.AppError
import civil.errors.AppError.DatabaseError
import civil.models._
import civil.models.actions.NeutralState
import civil.database.queries.CommentQueries.{getCommentsWithReplies, getCommentsWithRepliesUnauthenticatedQuery}
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

  def getCommentsUnauthenticated(
      discussionId: UUID,
      skip: Int
  ): ZIO[Any, AppError, List[CommentNode]]

  def getComment(
      userId: String,
      commentId: UUID
  ): ZIO[Any, AppError, CommentReplyWithParent]

  def getAllCommentReplies(
      userId: String,
      commentId: UUID
  ): ZIO[Any, AppError, CommentWithReplies]

  def getAllCommentRepliesUnauthenticated(
      commentId: UUID
  ): ZIO[Any, AppError, CommentWithReplies]

  def getUserComments(
      requestingUserId: String,
      userId: String,
      skip: Int
  ): ZIO[Any, AppError, List[CommentNode]]

  def getUserCommentsUnauthenticated(
      userId: String,
      skip: Int
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

  def getCommentsUnauthenticated(
      discussionId: UUID,
      skip: Int
  ): ZIO[CommentsRepository, AppError, List[CommentNode]] =
    ZIO.serviceWithZIO[CommentsRepository](
      _.getCommentsUnauthenticated(discussionId, skip)
    )

  def getComment(
      userId: String,
      commentId: UUID
  ): ZIO[CommentsRepository, AppError, CommentReplyWithParent] =
    ZIO.serviceWithZIO[CommentsRepository](_.getComment(userId, commentId))

  def getAllCommentReplies(
      userId: String,
      commentId: UUID
  ): ZIO[CommentsRepository, AppError, CommentWithReplies] =
    ZIO.serviceWithZIO[CommentsRepository](
      _.getAllCommentReplies(userId, commentId)
    )

  def getAllCommentRepliesUnauthenticated(
      commentId: UUID
  ): ZIO[CommentsRepository, AppError, CommentWithReplies] =
    ZIO.serviceWithZIO[CommentsRepository](
      _.getAllCommentRepliesUnauthenticated(commentId)
    )
  def getUserComments(
      requestingUserId: String,
      userId: String,
      skip: Int
  ): ZIO[CommentsRepository, AppError, List[CommentNode]] =
    ZIO.serviceWithZIO[CommentsRepository](
      _.getUserComments(requestingUserId, userId, skip)
    )

  def getUserCommentsUnauthenticated(
      userId: String,
      skip: Int
  ): ZIO[CommentsRepository, AppError, List[CommentNode]] =
    ZIO.serviceWithZIO[CommentsRepository](
      _.getUserCommentsUnauthenticated(userId, skip)
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
      ).mapError(DatabaseError(_))
        .provideEnvironment(ZEnvironment(dataSource))

    } yield inserted
      .into[CommentReply]
      .withFieldConst(_.likeState, NeutralState)
      .withFieldConst(_.createdByUserData, CreatedByUserData(
        createdByUsername = requestingUserData.username,
        createdByUserId = requestingUserData.userId,
        createdByIconSrc = requestingUserData.userIconSrc,
        createdByTag = requestingUserData.userCivilTag.some,
        civilityPoints = 0,
        numFollowers = None,
        numFollowed = None,
        numPosts = None
      ))
      .withFieldConst(_.civility, 0.0f)
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
        .mapError(DatabaseError(_))
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
                      c.createdByTag,
                      c.createdByUsername,
                      c.createdByCivility,
                      c.numFollowers.some
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
                      c.createdByTag,
                      c.createdByUsername,
                      c.createdByCivility,
                      c.numFollowers.some
                    )
                  )

                tc = CommentsTreeConstructor
                replyTree = tc.construct(repliesWithDepth, replies).head

              } yield replyTree
            }
        )
        .mapError(DatabaseError(_))
    } yield commentsWithReplies).provideEnvironment(ZEnvironment(dataSource))

  }

  override def getCommentsUnauthenticated(
      discussionId: UUID,
      skip: Int
  ): ZIO[Any, AppError, List[CommentNode]] = {
    (for {
      joinedDataQuery <- run {
        query[Comments]
          .filter(c =>
            c.discussionId == lift(discussionId) && c.parentId.isEmpty
          )
          .sortBy(comment => comment.createdAt)(Ord.desc)
          .drop(lift(skip))
          .take(10)
      }
        .mapError(DatabaseError(_))
      commentsWithReplies <- ZIO
        .collectAll(
          joinedDataQuery
            .map { case (comment) =>
              for {
                comments <- run(
                  getCommentsWithRepliesUnauthenticatedQuery(lift(comment.id))
                )
                repliesWithDepth = comments
                  .map(c =>
                    Comments.commentToCommentReplyWithDepth(
                      c.transformInto[CommentWithDepth],
                      NeutralState,
                      0,
                      c.userIconSrc.getOrElse(""),
                      c.userId,
                      c.userExperience,
                      c.createdByTag,
                      c.createdByUsername,
                      c.createdByCivility,
                      c.numFollowers.some
                    )
                  )
                  .reverse
                replies = comments
                  .map(c =>
                    Comments.commentToCommentReply(
                      c.transformInto[CommentWithDepth],
                      NeutralState,
                      0,
                      c.userIconSrc.getOrElse(""),
                      c.userId,
                      c.userExperience,
                      c.createdByTag,
                      c.createdByUsername,
                      c.createdByCivility,
                      c.numFollowers.some
                    )
                  )

                tc = CommentsTreeConstructor
                replyTree = tc.construct(repliesWithDepth, replies).head

              } yield replyTree
            }
        )
        .mapError(DatabaseError(_))
    } yield commentsWithReplies).provideEnvironment(ZEnvironment(dataSource))
  }

  override def getComment(
      userId: String,
      commentId: UUID
  ): ZIO[Any, AppError, CommentReplyWithParent] = {
    (for {
      commentWithUserDataAndParent <- run(
        query[Comments]
          .filter(c => c.id == lift(commentId))
          .join(query[Users])
          .on(_.createdByUserId == _.userId)
          .leftJoin(query[Comments])
          .on((cu, parentComment) => cu._1.parentId.contains(parentComment.id))
      ).head

      ((comment, user), parentCommentOpt) = commentWithUserDataAndParent
      //      user <- run(query[Users].filter(u => u.userId == lift(userId)))
      //      userData <- ZIO
      //        .fromOption(user.headOption)
    } yield comment
      .into[CommentReplyWithParent]
      .withFieldConst(_.createdByIconSrc, user.iconSrc.getOrElse(""))
      .withFieldConst(_.createdByUserId, user.userId)
      .withFieldConst(_.createdByExperience, user.experience)
      .withFieldConst(_.createdByTag, user.tag)
      .withFieldConst(_.likeState, NeutralState)
      .withFieldConst(_.civility, 0f)
      .withFieldConst(_.parentComment, parentCommentOpt)
      .transform)
      .mapError(_ => DatabaseError(new Throwable("Error getting comment")))
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
        ).mapError(DatabaseError(_))
      commentUserData <- ZIO
        .fromOption(commentUser.headOption)
        .orElseFail(DatabaseError(new Throwable("error")))

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
        .mapError(DatabaseError(_))
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
                      c.createdByTag,
                      c.createdByUsername,
                      c.createdByCivility,
                      c.numFollowers.some
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
                      c.createdByTag,
                      c.createdByUsername,
                      c.createdByCivility,
                      c.numFollowers.some
                    )
                  )

                tc = CommentsTreeConstructor
                replyTree = tc.construct(repliesWithDepth, replies).toList.head
              } yield replyTree
            }
        )
        .mapError(DatabaseError(_))
    } yield CommentWithReplies(
      replies = commentsWithReplies,
      comment = comment
        .into[CommentReply]
        .withFieldConst(
          _.likeState,
          likeOpt.map(_.likeState).getOrElse(NeutralState)
        )
        .withFieldConst(_.createdByUserData, CreatedByUserData(
          createdByUsername = user.username,
          createdByUserId = user.userId,
          createdByIconSrc = user.iconSrc.getOrElse(""),
          createdByTag = user.tag,
          civilityPoints = user.civility.toLong,
          numFollowers = None,
          numFollowed = None,
          numPosts = None,
          createdByExperience = user.experience
        ))
        .withFieldConst(_.civility, civilityOpt.map(_.value).getOrElse(0f))
        .transform
    )).provideEnvironment(ZEnvironment(dataSource))

  }

  override def getAllCommentRepliesUnauthenticated(
      commentId: UUID
  ): ZIO[Any, AppError, CommentWithReplies] = {
    (for {
      commentUser <-
        run(
          query[Comments]
            .filter(c => c.id == lift(commentId))
            .join(query[Users])
            .on(_.createdByUserId == _.userId)
        ).mapError(DatabaseError(_))
      commentUserData <- ZIO
        .fromOption(commentUser.headOption)
        .orElseFail(DatabaseError(new Throwable("error")))

      (comment, user) = commentUserData
      joinedData <- run {
        query[Comments]
          .filter(c => c.parentId == lift(Option(commentId)))
          .sortBy { case (comment) => comment.createdAt }(Ord.desc)
      }
        .mapError(DatabaseError(_))
      commentsWithReplies <- ZIO
        .collectAll(
          joinedData
            .map(comment =>
              for {
                comments <- run(
                  getCommentsWithRepliesUnauthenticatedQuery(lift(comment.id))
                )
                repliesWithDepth = comments
                  .map(c =>
                    Comments.commentToCommentReplyWithDepth(
                      c.transformInto[CommentWithDepth],
                      NeutralState,
                      0,
                      c.userIconSrc.getOrElse(""),
                      c.userId,
                      c.userExperience,
                      c.createdByTag,
                      c.createdByUsername,
                      c.createdByCivility,
                      c.numFollowers.some
                    )
                  )
                  .reverse

                replies = comments
                  .map(c =>
                    Comments.commentToCommentReply(
                      c.transformInto[CommentWithDepth],
                      NeutralState,
                      0,
                      c.userIconSrc.getOrElse(""),
                      c.userId,
                      c.userExperience,
                      c.createdByTag,
                      c.createdByUsername,
                      c.createdByCivility,
                      c.numFollowers.some
                    )
                  )

                tc = CommentsTreeConstructor
                replyTree = tc.construct(repliesWithDepth, replies).toList.head
              } yield replyTree
            )
        )
        .mapError(DatabaseError(_))
    } yield CommentWithReplies(
      replies = commentsWithReplies,
      comment = comment
        .into[CommentReply]
        .withFieldConst(
          _.likeState,
          NeutralState
        )
        .withFieldConst(_.createdByUserData, CreatedByUserData(
          createdByUsername = user.username,
          createdByUserId = user.userId,
          createdByIconSrc = user.iconSrc.getOrElse(""),
          createdByTag = user.tag,
          civilityPoints = user.civility.toLong,
          numFollowers = None,
          numFollowed = None,
          numPosts = None,
          createdByExperience = user.experience
        ))
        .withFieldConst(_.civility, 0f)
        .transform
    )).provideEnvironment(ZEnvironment(dataSource))
  }

  override def getUserComments(
      requestingUserId: String,
      userId: String,
      skip: Int
  ): ZIO[Any, AppError, List[CommentNode]] = {

    (for {
      commentsUsersJoin <-
        run(
          query[Comments]
            .filter(c => c.createdByUserId == lift(userId))
            .join(query[Users])
            .on(_.createdByUserId == _.userId)
            .drop(lift(skip))
            .take(5)
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
                      c.createdByTag,
                      c.createdByUsername,
                      c.createdByCivility,
                      c.numFollowers.some
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
                      c.createdByTag,
                      c.createdByUsername,
                      c.createdByCivility,
                      c.numFollowers.some
                    )
                  )

                tc = CommentsTreeConstructor
                replyTree = tc.construct(repliesWithDepth, replies).toList.head
              } yield replyTree
            })
        )

    } yield commentsWithReplies)
      .mapError(DatabaseError(_))
      .provideEnvironment(ZEnvironment(dataSource))

  }

  override def getUserCommentsUnauthenticated(
      userId: String,
      skip: Int
  ): ZIO[Any, AppError, List[CommentNode]] = {
    (for {
      commentsUsersJoin <-
        run(
          query[Comments]
            .filter(c => c.createdByUserId == lift(userId))
            .join(query[Users])
            .on(_.createdByUserId == _.userId)
            .drop(lift(skip))
            .take(5)
        )

      rootComments = commentsUsersJoin.filter(j => j._1.parentId.isEmpty)

      commentsWithReplies <- ZIO
        .collectAll(
          rootComments
            .map(joined => {
              val c = joined._1

              for {
                comments <- run(
                  getCommentsWithRepliesUnauthenticatedQuery(lift(c.id))
                )
                repliesWithDepth = comments
                  .map(c =>
                    Comments.commentToCommentReplyWithDepth(
                      c.transformInto[CommentWithDepth],
                      NeutralState,
                      0,
                      c.userIconSrc.getOrElse(""),
                      c.userId,
                      c.userExperience,
                      c.createdByTag,
                      c.createdByUsername,
                      c.createdByCivility,
                      c.numFollowers.some
                    )
                  )
                  .reverse
                replies = comments
                  .map(c =>
                    Comments.commentToCommentReply(
                      c.transformInto[CommentWithDepth],
                      NeutralState,
                      0,
                      c.userIconSrc.getOrElse(""),
                      c.userId,
                      c.userExperience,
                      c.createdByTag,
                      c.createdByUsername,
                      c.createdByCivility,
                      c.numFollowers.some
                    )
                  )

                tc = CommentsTreeConstructor
                replyTree = tc.construct(repliesWithDepth, replies).toList.head
              } yield replyTree
            })
        )

    } yield commentsWithReplies)
      .mapError(DatabaseError(_))
      .provideEnvironment(ZEnvironment(dataSource))
  }
}

object CommentsRepositoryLive {
  val layer: URLayer[DataSource, CommentsRepository] =
    ZLayer.fromFunction(CommentsRepositoryLive.apply _)

}
