package civil.repositories.comments

import civil.models.NotifcationEvents.CommentLike
import civil.models.{
  CommentLiked,
  CommentLikes,
  Comments,
  ErrorInfo,
  InternalServerError,
  TribunalComments
}
import civil.models._
import civil.services.KafkaProducerServiceLive
import zio.{Has, ZIO, ZLayer}

trait CommentLikesRepository {
  def addRemoveCommentLikeOrDislike(
      commentLikeDislike: CommentLikes,
      createdById: String
  ): ZIO[Any, ErrorInfo, (CommentLiked, Comments)]

  def addRemoveTribunalCommentLikeOrDislike(
      commentLikeDislike: CommentLikes
  ): ZIO[Any, ErrorInfo, CommentLiked]
}

object CommentLikesRepository {
  def addRemoveCommentLikeOrDislike(
      commentLikeDislike: CommentLikes,
      createdById: String
  ): ZIO[Has[CommentLikesRepository], ErrorInfo, (CommentLiked, Comments)] =
    ZIO.serviceWith[CommentLikesRepository](
      _.addRemoveCommentLikeOrDislike(commentLikeDislike, createdById)
    )

  def addRemoveTribunalCommentLikeOrDislike(
      commentLikeDislike: CommentLikes
  ): ZIO[Has[CommentLikesRepository], ErrorInfo, CommentLiked] =
    ZIO.serviceWith[CommentLikesRepository](
      _.addRemoveTribunalCommentLikeOrDislike(commentLikeDislike)
    )

}

case class CommentLikesRepositoryLive() extends CommentLikesRepository {
  import civil.repositories.QuillContextHelper.ctx._

  override def addRemoveCommentLikeOrDislike(
      commentLikeDislike: CommentLikes,
      createdById: String
  ): ZIO[Any, ErrorInfo, (CommentLiked, Comments)] = {
    for {
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
  ): ZIO[Any, ErrorInfo, CommentLiked] = {
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
          )
        )
        .mapError(e => InternalServerError(e.toString))
      newLikeState = commentLikeDislike.value
      prevLikeState = previousLikeState.headOption
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
    } yield likeValueToAdd
  }

}

object CommentLikesRepositoryLive {
  val live: ZLayer[Any, Nothing, Has[CommentLikesRepository]] =
    ZLayer.succeed(CommentLikesRepositoryLive())
}
