package civil.repositories.recommendations

import civil.models.{OutgoingRecommendations, Recommendations, Discussions, Topics}
import civil.errors.AppError
import civil.errors.AppError.InternalServerError
import civil.repositories.QuillContextHelper
import io.getquill.Ord
import zio._
import io.scalaland.chimney.dsl._

import java.util.UUID

trait RecommendationsRepository {
  def insertRecommendation(rec: Recommendations): Task[Unit]
  def batchInsertRecommendation(recs: List[Recommendations]): Task[Unit]
  def getAllRecommendations(targetContentId: UUID): ZIO[Any, AppError, List[OutgoingRecommendations]]
}

object RecommendationsRepository {
  def insertRecommendation(rec: Recommendations): RIO[RecommendationsRepository, Unit] =
    ZIO.serviceWithZIO[RecommendationsRepository](_.insertRecommendation(rec))
  def batchInsertRecommendation(recs: List[Recommendations]): RIO[RecommendationsRepository, Unit] =
    ZIO.serviceWithZIO[RecommendationsRepository](_.batchInsertRecommendation(recs))
  def getAllRecommendations(targetContentId: UUID): ZIO[RecommendationsRepository, AppError, List[OutgoingRecommendations]] =
    ZIO.serviceWithZIO[RecommendationsRepository](_.getAllRecommendations(targetContentId))
}


case class RecommendationsRepositoryLive() extends RecommendationsRepository {
  import QuillContextHelper.ctx._
  import QuillContextHelper.ctx.extras._

  override def insertRecommendation(rec: Recommendations): Task[Unit] = {
    run(query[Recommendations].insertValue(lift(rec)))
    ZIO.unit
  }

  override def batchInsertRecommendation(recs: List[Recommendations]): Task[Unit] = {
    run(liftQuery(recs).foreach(e => query[Recommendations].insertValue(e)))
    ZIO.unit
  }

  override def getAllRecommendations(targetContentId: UUID): ZIO[Any, AppError, List[OutgoingRecommendations]] = {


    val q = quote {
      for {
        rec <- query[Recommendations].filter(rec => rec.targetContentId == lift(targetContentId)).sortBy(r => r.similarityScore)(Ord.descNullsLast)
        t <- query[Topics].leftJoin(t => t.id === rec.recommendedContentId)
        st <- query[Discussions].leftJoin(st => st.id === rec.recommendedContentId)
      } yield (rec, t, st)
    }

    for {
      outgoingRecs <- ZIO.attempt(
        run(q).map({ case (rec, topic, subtopic) =>
          rec.into[OutgoingRecommendations]
            .withFieldConst(_.topic, topic)
            .withFieldConst(_.discussion, subtopic)
            .transform
        })
      ).mapError(e => InternalServerError(e.toString))
    } yield outgoingRecs
  }

}

object RecommendationsRepositoryLive {
  val layer: URLayer[Any, RecommendationsRepository] = ZLayer.fromFunction(RecommendationsRepositoryLive.apply _)
}