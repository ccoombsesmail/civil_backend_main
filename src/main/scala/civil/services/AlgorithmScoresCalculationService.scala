package civil.services

import civil.errors.AppError
import civil.errors.AppError.{DatabaseError, InternalServerError}
import civil.models.{
  CommentWithDepthAndUser,
  Comments,
  DiscussionLikes,
  Discussions
}
import civil.models.actions.{DislikedState, LikedState}
import civil.repositories.QuillContextQueries.getLikeRatio
import io.getquill.{Action, Query}
import zio.{IO, URLayer, ZEnvironment, ZIO, ZLayer}

import java.time.{ZoneOffset, ZonedDateTime}
import java.util.UUID
import javax.sql.DataSource

trait AlgorithmScoresCalculationService {
  def calculatePopularityScore(
      discussionId: UUID
  ): ZIO[Any, InternalServerError, Unit]
}

object AlgorithmScoresCalculationService {
  def calculatePopularityScore(
      discussionId: UUID
  ): ZIO[AlgorithmScoresCalculationService, AppError, Unit] =
    ZIO.serviceWithZIO[AlgorithmScoresCalculationService](
      _.calculatePopularityScore(discussionId)
    )
}

case class AlgorithmScoresCalculationServiceLive(dataSource: DataSource)
    extends AlgorithmScoresCalculationService {

  import civil.repositories.QuillContext._

  private final val SMALL_OFFSET = 1

  def calculatePopularityScore(
      targetDiscussionId: UUID
  ) = {

    case class DiscussionCommentCount(
        discussionId: UUID,
        createdAt: ZonedDateTime,
        comments: Long
    )

    val commentsQuery = quote {
      query[Discussions]
        .filter(_.id == lift(targetDiscussionId))
        .leftJoin(query[Comments])
        .on(_.id == _.discussionId)
        .groupBy { case (d, c) => (d.id, d.createdAt) }
        .map { case ((discussionId, createdAt), comments) =>
          (discussionId, createdAt, comments.size)
        }
        .map { case (discussionId, createdAt, commentCount) =>
          DiscussionCommentCount(discussionId, createdAt, commentCount)
        }
    }

    (for {
      ratio <- run(getLikeRatio(lift(targetDiscussionId))).head
      _ <- ZIO.logInfo(s"Like Ratio $ratio ")
      comments <- run(commentsQuery).head
      timeBetween = 1.0 / (SMALL_OFFSET + (ZonedDateTime
        .now(ZoneOffset.UTC)
        .toEpochSecond - comments.createdAt.toEpochSecond))

      commentNumber = if (comments.comments == 0) 1 else comments.comments
      popScore = Math.pow(ratio, 4) * timeBetween * Math.pow(
        commentNumber.toDouble,
        4
      )
      _ <- ZIO.logInfo(s"Popularity Score $popScore ")
      _ <- run(
        query[Discussions]
          .filter(_.id == lift(targetDiscussionId))
          .update(d => d.popularityScore -> lift(popScore))
      )
    } yield ())
      .orElseFail(InternalServerError(new Throwable("Calc error")))
      .provideEnvironment(ZEnvironment(dataSource))

  }

}

object AlgorithmScoresCalculationServiceLive {
  val layer: URLayer[DataSource, AlgorithmScoresCalculationServiceLive] =
    ZLayer.fromFunction(AlgorithmScoresCalculationServiceLive.apply _)
}
