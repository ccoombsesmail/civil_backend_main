package civil.repositories.discussions

import civil.errors.AppError.{DatabaseError, InternalServerError}
import civil.errors.AppError
import civil.models.actions.{
  DislikedState,
  LikeAction,
  LikedState,
  NeutralState
}
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

case class DiscussionLikesRepositoryLive(dataSource: DataSource)
    extends DiscussionLikesRepository {

  import civil.repositories.QuillContext._

  override def addRemoveDiscussionLikeOrDislike(
      discussionLikeDislikeData: UpdateDiscussionLikes,
      userId: String
  ): ZIO[Any, AppError, (DiscussionLiked, Discussions)] = {
    (for {
      previousLikeState <- run(
        query[DiscussionLikes].filter(tl =>
          tl.discussionId == lift(
            discussionLikeDislikeData.id
          ) && tl.userId == lift(userId)
        )
      ).mapError(DatabaseError)
      newLikeState = discussionLikeDislikeData.likeAction
      prevLikeState = previousLikeState.headOption
        .getOrElse(
          DiscussionLikes(discussionLikeDislikeData.id, userId, NeutralState)
        )
        .likeState

      likeValueToAdd <- ZIO.fromEither(computeLikeValue(prevLikeState, newLikeState))

      discussion <- transaction {
        for {
          _ <- run(
            query[DiscussionLikes]
              .insertValue(
                lift(
                  DiscussionLikes(
                    discussionLikeDislikeData.id,
                    userId,
                    discussionLikeDislikeData.likeAction
                  )
                )
              )
              .onConflictUpdate(_.discussionId, _.userId)((t, e) =>
                t.likeState -> e.likeState
              )
              .returning(r => r)
          )
          updatedDiscussion <- run(
            query[Discussions]
              .filter(t => t.id == lift(discussionLikeDislikeData.id))
              .update(discussion =>
                discussion.likes -> (discussion.likes + lift(likeValueToAdd))
              )
              .returning(t => t)
          )
        } yield updatedDiscussion
      }.mapError(e => {
        println(e)
        DatabaseError(e)
      })
    } yield (
      DiscussionLiked(
        discussion.id,
        discussion.likes,
        discussionLikeDislikeData.likeAction
      ),
      discussion
    )).provideEnvironment(ZEnvironment(dataSource))
  }

  private def computeLikeValue(prevState: LikeAction, newState: LikeAction): Either[InternalServerError, Int] = {
    (prevState, newState) match {
      case (LikedState, NeutralState) => Right(-1)
      case (NeutralState, LikedState) => Right(1)
      case (DislikedState, NeutralState) => Right(1)
      case (NeutralState, DislikedState) => Right(-1)
      case (LikedState, DislikedState) => Right(-2)
      case (DislikedState, LikedState) => Right(2)
      case (NeutralState, NeutralState) => Right(0)
      case _ => Left(InternalServerError(new Throwable(s"Invalid like value: Prev -> $prevState New -> $newState")))
    }
  }


}

object DiscussionLikesRepositoryLive {

  val layer: URLayer[DataSource, DiscussionLikesRepository] =
    ZLayer.fromFunction(DiscussionLikesRepositoryLive.apply _)

}
