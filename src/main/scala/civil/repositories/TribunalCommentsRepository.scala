package civil.repositories

import civil.errors.AppError
import civil.errors.AppError.DatabaseError
import civil.models._
import civil.models.actions.NeutralState
import civil.models.enums.TribunalCommentType
import civil.database.queries.CommentQueries.{
  getTribunalCommentsWithReplies,
  getTribunalCommentsWithRepliesUnauthenticatedQuery
}
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

  def getCommentsUnauthenticated(
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
    ZIO.serviceWithZIO[TribunalCommentsRepository](
      _.insertComment(comment, commentCreatorData)
    )

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

  def getCommentsUnauthenticated(
      contentId: UUID,
      commentType: TribunalCommentType
  ): ZIO[TribunalCommentsRepository, AppError, List[
    TribunalCommentNode
  ]] =
    ZIO.serviceWithZIO[TribunalCommentsRepository](
      _.getCommentsUnauthenticated(contentId, commentType)
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
          .filter(r =>
            r.userId == lift(comment.createdByUserId) && r.contentId == lift(
              comment.reportedContentId
            )
          )
      )
      isReporter = userReport.nonEmpty
      _ <- ZIO.logInfo(s"isReporter: $isReporter")
      jury <- run(
        query[TribunalJuryMembers]
          .filter(jm =>
            jm.userId == lift(comment.createdByUserId) && jm.contentId == lift(
              comment.reportedContentId
            )
          )
      )
      j = jury.headOption
      _ <- ZIO.logInfo(s"Is Jury Member: ${j.isDefined}")
      discussion <- run(
        query[Discussions].filter(d =>
          d.id == lift(comment.reportedContentId) && d.createdByUserId == lift(
            comment.createdByUserId
          )
        )
      )
      d = discussion.headOption
      _ <- ZIO.logInfo(s"Discussion exists: ${d.isDefined}")

      commentQuery <- run(
        query[Comments].filter(d =>
          d.id == lift(comment.reportedContentId) && d.createdByUserId == lift(
            comment.createdByUserId
          )
        )
      )
      c = commentQuery.headOption
      _ <- ZIO.logInfo(s"Comment exists: ${c.isDefined}")

      space <- run(
        query[Spaces].filter(d =>
          d.id == lift(comment.reportedContentId) && d.createdByUserId == lift(
            comment.createdByUserId
          )
        )
      )
      s = space.headOption
      _ <- ZIO.logInfo(s"Space exists: ${s.isDefined}")

      commentType =
        if (c.isDefined || s.isDefined || d.isDefined)
          TribunalCommentType.Defendant
        else if (j.isDefined)
          TribunalCommentType.Jury
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
      .transform)
      .mapError(DatabaseError(_))
      .provideEnvironment(ZEnvironment(dataSource))

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

  override def getCommentsUnauthenticated(
      contentId: UUID,
      commentType: TribunalCommentType
  ): ZIO[Any, AppError, List[TribunalCommentNode]] = {
    if (commentType == TribunalCommentType.All)
      getAllCommentsUnauthenticated(contentId)
    else
      getCommentsByCommentTypeUnauthenticated(contentId, commentType)
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
  ): ZIO[Any, AppError, List[TribunalCommentNode]] = (for {
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
    ).mapError(DatabaseError(_))
    commentsNodes <- ZIO
      .collectAll(commentsWithUserLikesCivility.map {
        case (comment, user, _, _) =>
          for {
            commentsWithDepth <- run(
              getTribunalCommentsWithReplies(lift(comment.id), lift(userId))
            ).mapError(DatabaseError(_))
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
            replyTree <- ZIO
              .fromOption(
                tc
                  .constructTribunal(repliesWithDepth, replies)
                  .headOption
              )
              .orElseFail(DatabaseError(new Throwable("sd")))
          } yield replyTree
      })
  } yield commentsNodes)
    .provideEnvironment(ZEnvironment(dataSource))
    .mapError(DatabaseError(_))

  private def getCommentsByCommentType(
      userId: String,
      contentId: UUID,
      commentType: TribunalCommentType
  ): ZIO[Any, AppError, List[TribunalCommentNode]] = (for {
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
    .mapError(DatabaseError(_))

  private def getAllCommentsUnauthenticated(
      contentId: UUID
  ): ZIO[Any, AppError, List[TribunalCommentNode]] = (for {
    commentsWithUserLikesCivility <- run(
      query[TribunalComments]
        .filter(c =>
          c.reportedContentId == lift(contentId) && c.parentId.isEmpty
        )
        .sortBy { case (comment) => comment.createdAt }(Ord.desc)
    ).mapError(DatabaseError(_))
    commentsNodes <- ZIO
      .collectAll(commentsWithUserLikesCivility.map { case (comment) =>
        for {
          commentsWithDepth <- run(
            getTribunalCommentsWithRepliesUnauthenticatedQuery(lift(comment.id))
          ).mapError(DatabaseError(_))
          repliesWithDepth = commentsWithDepth.map { c =>
            TribunalComments.withDepthToReplyWithDepth(
              c.into[TribunalCommentWithDepthAndUser]
                .withFieldConst(_.likeState, Some(NeutralState))
                .withFieldConst(_.civility, Some(0f))
                .transform,
              NeutralState,
              0f,
              c.userIconSrc.getOrElse(""),
              c.userId,
              c.userExperience,
              c.createdByTag
            )
          }.reverse

          replies = commentsWithDepth.map { c =>
            TribunalComments.commentToCommentReply(
              c.into[TribunalCommentWithDepthAndUser]
                .withFieldConst(_.likeState, Some(NeutralState))
                .withFieldConst(_.civility, Some(0f))
                .transform,
              NeutralState,
              0f,
              c.userIconSrc.getOrElse(""),
              c.userId,
              c.userExperience,
              c.createdByTag
            )
          }
          tc = CommentsTreeConstructor
          replyTree <- ZIO
            .fromOption(
              tc
                .constructTribunal(repliesWithDepth, replies)
                .headOption
            )
            .orElseFail(DatabaseError(new Throwable("sd")))
        } yield replyTree
      })
  } yield commentsNodes)
    .provideEnvironment(ZEnvironment(dataSource))
    .mapError(DatabaseError(_))

  private def getCommentsByCommentTypeUnauthenticated(
      contentId: UUID,
      commentType: TribunalCommentType
  ): ZIO[Any, AppError, List[TribunalCommentNode]] = (for {
    commentsWithUserLikesCivility <- run(
      query[TribunalComments]
        .filter(c =>
          c.reportedContentId == lift(contentId) && c.parentId.isEmpty
        )
        .filter(c => c.commentType == lift(commentType))
    )
    commentsNodes <- ZIO
      .collectAll(commentsWithUserLikesCivility.map { case (comment) =>
        for {
          commentsWithDepth <- run(
            getTribunalCommentsWithRepliesUnauthenticatedQuery(lift(comment.id))
          )
          repliesWithDepth = commentsWithDepth.map { c =>
            TribunalComments.withDepthToReplyWithDepth(
              c.into[TribunalCommentWithDepthAndUser]
                .withFieldConst(_.likeState, Some(NeutralState))
                .withFieldConst(_.civility, Some(0f))
                .transform,
              NeutralState,
              0f,
              c.userIconSrc.getOrElse(""),
              c.userId,
              c.userExperience,
              c.createdByTag
            )
          }.reverse
          replies = commentsWithDepth.map { c =>
            TribunalComments.commentToCommentReply(
              c.into[TribunalCommentWithDepthAndUser]
                .withFieldConst(_.likeState, Some(NeutralState))
                .withFieldConst(_.civility, Some(0f))
                .transform,
              NeutralState,
              0f,
              c.userIconSrc.getOrElse(""),
              c.userId,
              c.userExperience,
              c.createdByTag
            )
          }
          tc = CommentsTreeConstructor
          replyTree = tc
            .constructTribunal(repliesWithDepth, replies)
            .head
        } yield replyTree
      })
  } yield commentsNodes)
    .provideEnvironment(ZEnvironment(dataSource))
    .mapError(DatabaseError(_))

}

object TribunalCommentsRepositoryLive {
  val layer: URLayer[DataSource, TribunalCommentsRepository] =
    ZLayer.fromFunction(TribunalCommentsRepositoryLive.apply _)
}
