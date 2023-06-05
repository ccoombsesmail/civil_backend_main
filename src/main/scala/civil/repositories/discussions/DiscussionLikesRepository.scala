package civil.repositories.discussions

import civil.errors.AppError.InternalServerError
import civil.errors.AppError
import civil.models.actions.{DislikedState, LikeAction, LikedState, NeutralState}
import civil.models.enums.Sentiment.NEUTRAL
import civil.models._
import zio.{URLayer, ZEnvironment, ZIO, ZLayer}

import javax.sql.DataSource

trait DiscussionLikesRepository {
  def addRemoveDiscussionLikeOrDislike(
      discussionLikeDislikeData: UpdateDiscussionLikes,
      userId: String
  ): ZIO[Any, AppError, (DiscussionLiked, Discussions)]
}

object DiscussionLikesRepository {
  def addRemoveDiscussionLikeOrDislike(
      discussionLikeDislikeData: UpdateDiscussionLikes,
      userId: String
  ): ZIO[DiscussionLikesRepository, AppError, (DiscussionLiked, Discussions)] =
    ZIO.serviceWithZIO[DiscussionLikesRepository](
      _.addRemoveDiscussionLikeOrDislike(discussionLikeDislikeData, userId)
    )

}

case class DiscussionLikesRepositoryLive(dataSource: DataSource) extends DiscussionLikesRepository {
  import civil.repositories.QuillContext._

  override def addRemoveDiscussionLikeOrDislike(
      discussionLikeDislikeData: UpdateDiscussionLikes,
      userId: String
  ): ZIO[Any, AppError, (DiscussionLiked, Discussions)] = {
   (for {
      previousLikeState <- run(
        query[DiscussionLikes].filter(tl => tl.discussionId == lift(discussionLikeDislikeData.id) && tl.userId == lift(userId))
      ).mapError(e => InternalServerError(e.toString))
      newLikeState = discussionLikeDislikeData.likeAction
      prevLikeState = previousLikeState.headOption.getOrElse(DiscussionLikes(discussionLikeDislikeData.id, userId, NeutralState)).likeState
      _ = println((prevLikeState, newLikeState) + Console.RED_B)
      _ = println(Console.RESET)
      likeValueToAdd = (prevLikeState, newLikeState) match {
        case (LikedState, NeutralState) => -1
        case (NeutralState, LikedState) => 1
        case (DislikedState, NeutralState) => 1
        case (NeutralState, DislikedState) => -1
        case (LikedState, DislikedState) => -2
        case (DislikedState, LikedState) => 2
        case (NeutralState, NeutralState) => 0
        case _ => -100
      }
      _ <- ZIO.when(likeValueToAdd == -100)(ZIO.fail(InternalServerError("Invalid like value")))
        .mapError(e => InternalServerError(e.toString))
        discussion <- transaction {
          for {
            _ <- run(
              query[DiscussionLikes]
                .insertValue(lift(DiscussionLikes(discussionLikeDislikeData.id, userId, discussionLikeDislikeData.likeAction)))
                .onConflictUpdate(_.discussionId, _.userId)((t, e) => t.likeState -> e.likeState)
                .returning(r => r)
            )
            updatedDiscussion <- run(query[Discussions]
              .filter(t => t.id == lift(discussionLikeDislikeData.id))
              .update(discussion => discussion.likes -> (discussion.likes + lift(likeValueToAdd)))
              .returning(t => t)
            )
          } yield updatedDiscussion
        }.mapError(e => InternalServerError(e.toString))
    } yield (DiscussionLiked(discussion.id, discussion.likes, discussionLikeDislikeData.likeAction), discussion)).provideEnvironment(ZEnvironment(dataSource))
  }

}

object DiscussionLikesRepositoryLive {

  val layer: URLayer[DataSource, DiscussionLikesRepository] = ZLayer.fromFunction(DiscussionLikesRepositoryLive.apply _)

}