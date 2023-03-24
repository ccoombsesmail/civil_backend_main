package civil.repositories.recommendations

import civil.models.{ErrorInfo, InternalServerError, OutgoingRecommendations, Recommendations, Discussions, Topics}
import civil.models._
import civil.repositories.QuillContextHelper
import io.getquill.Ord
import zio._
import io.scalaland.chimney.dsl._

import java.util.UUID

trait RecommendationsRepository {
  def insertRecommendation(rec: Recommendations): Task[Unit]
  def batchInsertRecommendation(recs: List[Recommendations]): Task[Unit]
  def getAllRecommendations(targetContentId: UUID): ZIO[Any, ErrorInfo, List[OutgoingRecommendations]]
}

object RecommendationsRepository {
  def insertRecommendation(rec: Recommendations): RIO[Has[RecommendationsRepository], Unit] =
    ZIO.serviceWith[RecommendationsRepository](_.insertRecommendation(rec))
  def batchInsertRecommendation(recs: List[Recommendations]): RIO[Has[RecommendationsRepository], Unit] =
    ZIO.serviceWith[RecommendationsRepository](_.batchInsertRecommendation(recs))
  def getAllRecommendations(targetContentId: UUID): ZIO[Has[RecommendationsRepository], ErrorInfo, List[OutgoingRecommendations]] =
    ZIO.serviceWith[RecommendationsRepository](_.getAllRecommendations(targetContentId))
}


case class RecommendationsRepositoryLive() extends RecommendationsRepository {
  import QuillContextHelper.ctx._
  import QuillContextHelper.ctx.extras._

  override def insertRecommendation(rec: Recommendations): Task[Unit] = {
    run(query[Recommendations].insert(lift(rec)))
    ZIO.unit
  }

  override def batchInsertRecommendation(recs: List[Recommendations]): Task[Unit] = {
    run(liftQuery(recs).foreach(e => query[Recommendations].insert(e)))
    ZIO.unit
  }

  override def getAllRecommendations(targetContentId: UUID): ZIO[Any, ErrorInfo, List[OutgoingRecommendations]] = {


    val q = quote {
      for {
        rec <- query[Recommendations].filter(rec => rec.targetContentId == lift(targetContentId)).sortBy(r => r.similarityScore)(Ord.descNullsLast)
        t <- query[Topics].leftJoin(t => t.id === rec.recommendedContentId)
        st <- query[Discussions].leftJoin(st => st.id === rec.recommendedContentId)
      } yield (rec, t, st)
    }

    for {
      outgoingRecs <- ZIO.effect(
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
  val live: ZLayer[Any, Throwable, Has[RecommendationsRepository]] =
    ZLayer.succeed(RecommendationsRepositoryLive())
}