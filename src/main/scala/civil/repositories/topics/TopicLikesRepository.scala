package civil.repositories.topics

import civil.models.{ErrorInfo, InternalServerError, TopicLiked, TopicLikes, Topics, UpdateTopicLikes}
import civil.models._
import civil.repositories.QuillContextHelper
import io.scalaland.chimney.dsl.TransformerOps
import zio.{Has, ZIO, ZLayer}

trait TopicLikesRepository {
  def addRemoveTopicLikeOrDislike(
      topicLikeDislikeData: UpdateTopicLikes,
      userId: String
  ): ZIO[Any, ErrorInfo, (TopicLiked, Topics)]
}

object TopicLikesRepository {
  def addRemoveTopicLikeOrDislike(
      topicLikeDislikeData: UpdateTopicLikes,
      userId: String
  ): ZIO[Has[TopicLikesRepository], ErrorInfo, (TopicLiked, Topics)] =
    ZIO.serviceWith[TopicLikesRepository](
      _.addRemoveTopicLikeOrDislike(topicLikeDislikeData, userId)
    )

}

case class TopicLikesRepositoryLive() extends TopicLikesRepository {
  import QuillContextHelper.ctx._

  override def addRemoveTopicLikeOrDislike(
      topicLikeDislikeData: UpdateTopicLikes,
      userId: String
  ): ZIO[Any, ErrorInfo, (TopicLiked, Topics)] = {
    for {
      previousLikeState <- ZIO.effect(run(
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
        topic <- ZIO.effect(
        transaction {
          run(
            query[TopicLikes]
              .insert(lift(TopicLikes(topicLikeDislikeData.id, userId, topicLikeDislikeData.value)))
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
  val live: ZLayer[Any, Nothing, Has[TopicLikesRepository]] =
    ZLayer.succeed(TopicLikesRepositoryLive())
}