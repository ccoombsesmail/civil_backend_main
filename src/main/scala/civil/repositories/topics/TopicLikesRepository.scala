package civil.repositories.topics

import civil.errors.AppError.InternalServerError
import civil.errors.AppError
import civil.models.actions.{DislikedState, LikeAction, LikedState, NeutralState}
import civil.models.enums.Sentiment.NEUTRAL
import civil.models.{TopicLiked, TopicLikes, Topics, UpdateTopicLikes}
import zio.{URLayer, ZEnvironment, ZIO, ZLayer}

import javax.sql.DataSource

trait TopicLikesRepository {
  def addRemoveTopicLikeOrDislike(
      topicLikeDislikeData: UpdateTopicLikes,
      userId: String
  ): ZIO[Any, AppError, (TopicLiked, Topics)]
}

object TopicLikesRepository {
  def addRemoveTopicLikeOrDislike(
      topicLikeDislikeData: UpdateTopicLikes,
      userId: String
  ): ZIO[TopicLikesRepository, AppError, (TopicLiked, Topics)] =
    ZIO.serviceWithZIO[TopicLikesRepository](
      _.addRemoveTopicLikeOrDislike(topicLikeDislikeData, userId)
    )

}

case class TopicLikesRepositoryLive(dataSource: DataSource) extends TopicLikesRepository {
  import civil.repositories.QuillContext._

  override def addRemoveTopicLikeOrDislike(
      topicLikeDislikeData: UpdateTopicLikes,
      userId: String
  ): ZIO[Any, AppError, (TopicLiked, Topics)] = {
   (for {
      previousLikeState <- run(
        query[TopicLikes].filter(tl => tl.topicId == lift(topicLikeDislikeData.id) && tl.userId == lift(userId))
      ).mapError(e => InternalServerError(e.toString))
      newLikeState = topicLikeDislikeData.likeAction
      prevLikeState = previousLikeState.headOption.getOrElse(TopicLikes(topicLikeDislikeData.id, userId, NeutralState)).likeState
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
        topic <- transaction {
          for {
            _ <- run(
              query[TopicLikes]
                .insertValue(lift(TopicLikes(topicLikeDislikeData.id, userId, topicLikeDislikeData.likeAction)))
                .onConflictUpdate(_.topicId, _.userId)((t, e) => t.likeState -> e.likeState)
                .returning(r => r)
            )
            updatedTopic <- run(query[Topics]
              .filter(t => t.id == lift(topicLikeDislikeData.id))
              .update(topic => topic.likes -> (topic.likes + lift(likeValueToAdd)))
              .returning(t => t)
            )
          } yield updatedTopic
        }.mapError(e => InternalServerError(e.toString))
    } yield (TopicLiked(topic.id, topic.likes, topicLikeDislikeData.likeAction), topic)).provideEnvironment(ZEnvironment(dataSource))
  }

}

object TopicLikesRepositoryLive {

  val layer: URLayer[DataSource, TopicLikesRepository] = ZLayer.fromFunction(TopicLikesRepositoryLive.apply _)

}