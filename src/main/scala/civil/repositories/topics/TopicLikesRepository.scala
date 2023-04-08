package civil.repositories.topics

import civil.errors.AppError.InternalServerError
import civil.errors.AppError
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
    for {
      previousLikeState <- run(
        query[TopicLikes].filter(tl => tl.topicId == lift(topicLikeDislikeData.id) && tl.userId == lift(userId))
      ).mapError(e => InternalServerError(e.toString)).provideEnvironment(ZEnvironment(dataSource))
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
        .mapError(e => InternalServerError(e.toString)).provideEnvironment(ZEnvironment(dataSource))
        topic <- transaction {
          run(
            query[TopicLikes]
              .insertValue(lift(TopicLikes(topicLikeDislikeData.id, userId, topicLikeDislikeData.value)))
              .onConflictUpdate(_.topicId, _.userId)((t, e) => t.value -> e.value)
              .returning(r => r)
          )
          run(query[Topics]
            .filter(t => t.id == lift(topicLikeDislikeData.id))
            .update(topic => topic.likes -> (topic.likes + lift(likeValueToAdd)))
            .returning(t => t)
          )
        }.mapError(e => InternalServerError(e.toString)).provideEnvironment(ZEnvironment(dataSource))
    } yield (TopicLiked(topic.id, topic.likes, topicLikeDislikeData.value), topic)
  }

}

object TopicLikesRepositoryLive {

  val layer: URLayer[DataSource, TopicLikesRepository] = ZLayer.fromFunction(TopicLikesRepositoryLive.apply _)

}