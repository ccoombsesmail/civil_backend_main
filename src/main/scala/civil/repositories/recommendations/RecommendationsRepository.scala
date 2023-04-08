package civil.repositories.recommendations

import civil.models.{Discussions, OutgoingRecommendations, Recommendations, Topics}
import civil.errors.AppError
import civil.errors.AppError.InternalServerError
import io.getquill.Ord
import zio._
import io.scalaland.chimney.dsl._

import java.util.UUID
import javax.sql.DataSource

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


case class RecommendationsRepositoryLive(dataSource: DataSource) extends RecommendationsRepository {
  import civil.repositories.QuillContext._
  import civil.repositories.QuillContext.extras._

  override def insertRecommendation(rec: Recommendations): Task[Unit] = {
    run(query[Recommendations].insertValue(lift(rec))).mapError(e => InternalServerError(e.toString))
      .provideEnvironment(ZEnvironment(dataSource))
    ZIO.unit
  }

  override def batchInsertRecommendation(recs: List[Recommendations]): Task[Unit] = {
    run(liftQuery(recs).foreach(e => query[Recommendations].insertValue(e))).mapError(e => InternalServerError(e.toString))
      .provideEnvironment(ZEnvironment(dataSource))
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
      outgoingRecs <- run(q).mapError(e => InternalServerError(e.toString))
        .provideEnvironment(ZEnvironment(dataSource))
      out = outgoingRecs.map { case (rec, topic, subtopic) =>
        rec.into[OutgoingRecommendations]
        .withFieldConst (_.topic, topic)
        .withFieldConst (_.discussion, subtopic)
        .transform
      }
    } yield out
  }

}

object RecommendationsRepositoryLive {
  val layer: URLayer[DataSource, RecommendationsRepository] = ZLayer.fromFunction(RecommendationsRepositoryLive.apply _)
}