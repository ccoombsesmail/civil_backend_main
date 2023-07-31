package civil.repositories.comments

import civil.errors.AppError.InternalServerError
import civil.errors.AppError
import civil.models.actions.{DislikedState, LikedState, NeutralState}
import civil.models.enums.Sentiment.NEUTRAL
import civil.models.{CommentLiked, CommentLikes, Comments, TribunalComments}
import civil.repositories.comments
import zio.{URLayer, ZEnvironment, ZIO, ZLayer}

import javax.sql.DataSource
trait CommentLikesRepository {
  def addRemoveCommentLikeOrDislike(
      commentLikeDislike: CommentLikes,
      createdById: String
  ): ZIO[Any, AppError, (CommentLiked, Comments)]

  def addRemoveTribunalCommentLikeOrDislike(
      commentLikeDislike: CommentLikes
  ): ZIO[Any, AppError, CommentLiked]
}

object CommentLikesRepository {
  def addRemoveCommentLikeOrDislike(
      commentLikeDislike: CommentLikes,
      createdById: String
  ): ZIO[CommentLikesRepository, AppError, (CommentLiked, Comments)] =
    ZIO.environmentWithZIO[CommentLikesRepository] { env =>
      env
        .get[CommentLikesRepository]
        .addRemoveCommentLikeOrDislike(commentLikeDislike, createdById)
    }

  def addRemoveTribunalCommentLikeOrDislike(
      commentLikeDislike: CommentLikes
  ): ZIO[CommentLikesRepository, AppError, CommentLiked] =
    ZIO.environmentWithZIO[CommentLikesRepository] { env =>
      env
        .get[CommentLikesRepository]
        .addRemoveTribunalCommentLikeOrDislike(commentLikeDislike)
    }

}

case class CommentLikesRepositoryLive(dataSource: DataSource)
    extends CommentLikesRepository {
  import civil.repositories.QuillContext._
  override def addRemoveCommentLikeOrDislike(
      commentLikeDislike: CommentLikes,
      createdById: String
  ): ZIO[Any, AppError, (CommentLiked, Comments)] = {
    for {
//      _ <- log.info(s"Fetching previous like state for comment id ${commentLikeDislike.commentId}")
      likeValueToAddSubtract <- getLikeValueToAddOrSubtract(commentLikeDislike)
      comment <-
        transaction {
          for {
            _ <- run(
              query[CommentLikes]
                .insertValue(
                  lift(
                    commentLikeDislike
                  )
                )
                .onConflictUpdate(_.commentId, _.userId)((t, e) =>
                  t.likeState -> e.likeState
                )
                .returning(r => r)
            )
            updatedComment <- run(
              query[Comments]
                .filter(c => c.id == lift(commentLikeDislike.commentId))
                .update(comment =>
                  comment.likes -> (comment.likes + lift(
                    likeValueToAddSubtract
                  ))
                )
                .returning(c => c)
            )
          } yield updatedComment
        }
          .mapError(e => InternalServerError(e.toString))
          .provideEnvironment(ZEnvironment(dataSource))
    } yield (
      CommentLiked(
        comment.id,
        comment.likes,
        commentLikeDislike.likeState,
        comment.rootId
      ),
      comment
    )
  }

  override def addRemoveTribunalCommentLikeOrDislike(
      commentLikeDislike: CommentLikes
  ): ZIO[Any, AppError, CommentLiked] = {
    for {
      likeValueToAddSubtract <- getLikeValueToAddOrSubtract(commentLikeDislike)
      commentLikesData <-
        transaction {
          for {
            _ <- run(
              query[CommentLikes]
                .insertValue(
                  lift(
                    commentLikeDislike
                  )
                )
                .onConflictUpdate(_.commentId, _.userId)((t, e) =>
                  t.likeState -> e.likeState
                )
                .returning(r => r)
            )
            updated <- run(
              query[TribunalComments]
                .filter(c => c.id == lift(commentLikeDislike.commentId))
                .update(comment =>
                  comment.likes -> (comment.likes + lift(
                    likeValueToAddSubtract
                  ))
                )
                .returning(c =>
                  CommentLiked(
                    c.id,
                    c.likes,
                    lift(commentLikeDislike.likeState),
                    c.rootId
                  )
                )
            )
          } yield updated
        }
          .mapError(e => InternalServerError(e.toString))
          .provideEnvironment(ZEnvironment(dataSource))
    } yield commentLikesData
  }

  private def getLikeValueToAddOrSubtract(
      commentLikeDislike: CommentLikes
  ): ZIO[Any, InternalServerError, Index] = {
    for {
      previousLikeState <- run(
        query[CommentLikes].filter(cl =>
          cl.commentId == lift(
            commentLikeDislike.commentId
          ) && cl.userId == lift(commentLikeDislike.userId)
        )
      )
        .mapError(e => InternalServerError(e.toString))
        .provideEnvironment(ZEnvironment(dataSource))
      previousLikeStateResult = previousLikeState.headOption

//      _ <- log.info(s"Previous like state for comment id ${commentLikeDislike.commentId} is: ${previousLikeState}")
      newLikeState = commentLikeDislike.likeState
      prevLikeState = previousLikeStateResult
        .getOrElse(
          CommentLikes(
            commentLikeDislike.commentId,
            commentLikeDislike.userId,
            NeutralState
          )
        )
        .likeState
      likeValueToAdd = (prevLikeState, newLikeState) match {
        case (LikedState, NeutralState)    => -1
        case (NeutralState, LikedState)    => 1
        case (DislikedState, NeutralState) => 1
        case (NeutralState, DislikedState) => -1
        case (LikedState, DislikedState)   => -2
        case (DislikedState, LikedState)   => 2
        case (NeutralState, NeutralState)  => 0
        case _                             => -100
      }
      _ <- ZIO.when(likeValueToAdd == -100)(
        ZIO.fail(InternalServerError("Invalid like value"))
      )
    } yield likeValueToAdd
  }

}

object CommentLikesRepositoryLive {
  val layer: URLayer[DataSource, CommentLikesRepository] =
    ZLayer.fromFunction(CommentLikesRepositoryLive.apply _)

}
