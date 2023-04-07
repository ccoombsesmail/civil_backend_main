package civil.repositories.topics

import civil.errors.AppError.InternalServerError
import civil.errors.AppError
import civil.models.{TopicLiked, TopicLikes, Topics, UpdateTopicLikes}
import civil.repositories.QuillContextHelper
import zio.{URLayer, ZIO, ZLayer}

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

case class TopicLikesRepositoryLive() extends TopicLikesRepository {
  import QuillContextHelper.ctx._

  override def addRemoveTopicLikeOrDislike(
      topicLikeDislikeData: UpdateTopicLikes,
      userId: String
  ): ZIO[Any, AppError, (TopicLiked, Topics)] = {
    for {
      previousLikeState <- ZIO.attempt(run(
        query[TopicLikes].filter(tl => tl.topicId == lift(topicLikeDislikeData.id) && tl.userId == lift(userId))
      )).mapError(e => InternalServerError(e.toString))
      newLikeState = topicLikeDislikeData.value
      prevLikeState = previousLikeState.headOption.getOrElse(TopicLikes(topicLikeDislikeData.id, userId, 0)).value
      stateCombo = s"$prevLikeState$newLikeState"
      likeValueToAdd = stateCombo match {
        case "10" => -1
        case "01" => 1
        case "-10" => 1
        case "0-1" =>  -1
        case "1-1" => -2
        case "-11" => 2
        case _ => 0
      }
      _ <- ZIO.when(likeValueToAdd == 0)(ZIO.fail(InternalServerError("Invalid like value")))
        topic <- ZIO.attempt(
        transaction {
          run(
            query[TopicLikes]
              .insertValue(TopicLikes(topicLikeDislikeData.id, userId, topicLikeDislikeData.value))
              .onConflictUpdate(_.topicId, _.userId)((t, e) => t.value -> e.value)
              .returning(r => r)
          )
          run(query[Topics]
            .filter(t => t.id == lift(topicLikeDislikeData.id))
            .update(topic => topic.likes -> (topic.likes + lift(likeValueToAdd)))
            .returning(t => t)
          )
        }
      ).mapError(e => InternalServerError(e.toString))
    } yield (TopicLiked(topic.id, topic.likes, topicLikeDislikeData.value), topic)
  }

}

object TopicLikesRepositoryLive {

  val layer: URLayer[Any, TopicLikesRepository] = ZLayer.fromFunction(TopicLikesRepositoryLive.apply _)

}