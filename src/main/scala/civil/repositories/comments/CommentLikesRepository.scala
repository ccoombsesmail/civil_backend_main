package civil.repositories.comments

import civil.errors.AppError.InternalServerError
import civil.errors.AppError
import civil.models.{CommentLiked, CommentLikes, Comments, AppError, TribunalComments}
import civil.repositories.comments
import zio.{URLayer, ZIO, ZLayer}
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
      env.get[CommentLikesRepository].addRemoveCommentLikeOrDislike(commentLikeDislike, createdById)
    }

  def addRemoveTribunalCommentLikeOrDislike(
      commentLikeDislike: CommentLikes
  ): ZIO[CommentLikesRepository, AppError, CommentLiked] =
    ZIO.environmentWithZIO[CommentLikesRepository] { env =>
      env.get[CommentLikesRepository].addRemoveTribunalCommentLikeOrDislike(commentLikeDislike)
    }

}

case class CommentLikesRepositoryLive() extends CommentLikesRepository {
  import civil.repositories.QuillContextHelper.ctx._

  override def addRemoveCommentLikeOrDislike(
      commentLikeDislike: CommentLikes,
      createdById: String
  ): ZIO[Any, AppError, (CommentLiked, Comments)] = {
    for {
//      _ <- log.info(s"Fetching previous like state for comment id ${commentLikeDislike.commentId}")
      likeValueToAddSubtract <- getLikeValueToAddOrSubtract(commentLikeDislike)
      comment <- ZIO
        .attempt(
          transaction {
            run(
              query[CommentLikes]
                .insertValue(
                  lift(
                    commentLikeDislike
                  )
                )
                .onConflictUpdate(_.commentId, _.userId)((t, e) =>
                  t.value -> e.value
                )
                .returning(r => r)
            )
            run(
              query[Comments]
                .filter(c => c.id == lift(commentLikeDislike.commentId))
                .update(comment =>
                  comment.likes -> (comment.likes + lift(
                    likeValueToAddSubtract
                  ))
                )
                .returning(c => c)
            )
          }
        )
        .mapError(e => InternalServerError(e.toString))
    } yield (
      CommentLiked(
        comment.id,
        comment.likes,
        commentLikeDislike.value,
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
      commentLikesData <- ZIO
        .attempt(
          transaction {
            run(
              query[CommentLikes]
                .insertValue(
                  lift(
                    commentLikeDislike
                  )
                )
                .onConflictUpdate(_.commentId, _.userId)((t, e) =>
                  t.value -> e.value
                )
                .returning(r => r)
            )
            run(
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
                    lift(commentLikeDislike.value),
                    c.rootId
                  )
                )
            )
          }
        )
        .mapError(e => InternalServerError(e.toString))
    } yield commentLikesData
  }

  def getLikeValueToAddOrSubtract(commentLikeDislike: CommentLikes) = {
    for {
      previousLikeState <- ZIO
        .attempt(
          run(
            query[CommentLikes].filter(cl =>
              cl.commentId == lift(
                commentLikeDislike.commentId
              ) && cl.userId == lift(commentLikeDislike.userId)
            )
          ).headOption
        )
        .mapError(e => InternalServerError(e.toString))
//      _ <- log.info(s"Previous like state for comment id ${commentLikeDislike.commentId} is: ${previousLikeState}")
      newLikeState = commentLikeDislike.value
      prevLikeState = previousLikeState
        .getOrElse(
          CommentLikes(
            commentLikeDislike.commentId,
            commentLikeDislike.userId,
            0
          )
        )
        .value
      stateCombo = s"$prevLikeState$newLikeState"
      likeValueToAdd = stateCombo match {
        case "10"  => -1
        case "01"  => 1
        case "-10" => 1
        case "0-1" => -1
        case "1-1" => -2
        case "-11" => 2
        case _     => 0
      }
      _ <- ZIO.when(likeValueToAdd == 0)(ZIO.fail(InternalServerError("Invalid like value")))
    } yield likeValueToAdd
  }

}

object CommentLikesRepositoryLive {
  val layer: URLayer[Any, CommentLikesRepository] = ZLayer.fromFunction(CommentLikesRepositoryLive.apply _)

}
