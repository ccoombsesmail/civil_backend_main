package civil.repositories.comments

import civil.models.{CommentLiked, CommentLikes, Comments, ErrorInfo, InternalServerError, TribunalComments}
import civil.repositories.comments
import zio.{Has, ZIO, ZLayer}
import zio.logging._
trait CommentLikesRepository {
  def addRemoveCommentLikeOrDislike(
      commentLikeDislike: CommentLikes,
      createdById: String
  ): ZIO[Any with Logging, ErrorInfo, (CommentLiked, Comments)]

  def addRemoveTribunalCommentLikeOrDislike(
      commentLikeDislike: CommentLikes
  ): ZIO[Any with Logging, ErrorInfo, CommentLiked]
}

object CommentLikesRepository {
  def addRemoveCommentLikeOrDislike(
      commentLikeDislike: CommentLikes,
      createdById: String
  ): ZIO[Has[CommentLikesRepository] with Logging, ErrorInfo, (CommentLiked, Comments)] =
    ZIO.accessM[Has[CommentLikesRepository] with Logging] { env =>
      env.get[CommentLikesRepository].addRemoveCommentLikeOrDislike(commentLikeDislike, createdById)
    }

  def addRemoveTribunalCommentLikeOrDislike(
      commentLikeDislike: CommentLikes
  ): ZIO[Has[CommentLikesRepository] with Logging, ErrorInfo, CommentLiked] =
    ZIO.accessM[Has[CommentLikesRepository] with Logging] { env =>
      env.get[CommentLikesRepository].addRemoveTribunalCommentLikeOrDislike(commentLikeDislike)
    }

}

case class CommentLikesRepositoryLive() extends CommentLikesRepository {
  import civil.repositories.QuillContextHelper.ctx._

  override def addRemoveCommentLikeOrDislike(
      commentLikeDislike: CommentLikes,
      createdById: String
  ): ZIO[Any with Logging, ErrorInfo, (CommentLiked, Comments)] = {
    for {
      _ <- log.info(s"Fetching previous like state for comment id ${commentLikeDislike.commentId}")
      likeValueToAddSubtract <- getLikeValueToAddOrSubtract(commentLikeDislike)
      comment <- ZIO
        .effect(
          transaction {
            run(
              query[CommentLikes]
                .insert(
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
  ): ZIO[Any with Logging, ErrorInfo, CommentLiked] = {
    for {
      likeValueToAddSubtract <- getLikeValueToAddOrSubtract(commentLikeDislike)
      commentLikesData <- ZIO
        .effect(
          transaction {
            run(
              query[CommentLikes]
                .insert(
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
        .effect(
          run(
            query[CommentLikes].filter(cl =>
              cl.commentId == lift(
                commentLikeDislike.commentId
              ) && cl.userId == lift(commentLikeDislike.userId)
            )
          ).headOption
        )
        .mapError(e => InternalServerError(e.toString))
      _ <- log.info(s"Previous like state for comment id ${commentLikeDislike.commentId} is: ${previousLikeState}")
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
  val live: ZLayer[Any, Nothing, Has[CommentLikesRepository]] =
    ZLayer.succeed(CommentLikesRepositoryLive())
}
