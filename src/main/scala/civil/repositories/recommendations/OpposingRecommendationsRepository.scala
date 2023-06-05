package civil.repositories.recommendations

import civil.models.{Discussions, OpposingRecommendations, OutGoingOpposingRecommendations, Spaces, UrlsForTFIDFConversion}
import civil.errors.AppError
import civil.errors.AppError.InternalServerError
import io.getquill.Ord
import zio._
import io.scalaland.chimney.dsl._

import java.util.UUID
import javax.sql.DataSource

trait OpposingRecommendationsRepository {
  def insertOpposingRecommendation(opposingRec: OpposingRecommendations): ZIO[Any, AppError, Unit]
  def getAllOpposingRecommendations(targetContentId: UUID): ZIO[Any, AppError, List[OutGoingOpposingRecommendations]]
}

object OpposingRecommendationsRepository {
  def insertOpposingRecommendation(opposingRec: OpposingRecommendations): ZIO[OpposingRecommendationsRepository, AppError, Unit] =
    ZIO.serviceWithZIO[OpposingRecommendationsRepository](_.insertOpposingRecommendation(opposingRec))
  def getAllOpposingRecommendations(targetContentId: UUID): ZIO[OpposingRecommendationsRepository, AppError, List[OutGoingOpposingRecommendations]] =
    ZIO.serviceWithZIO[OpposingRecommendationsRepository](_.getAllOpposingRecommendations(targetContentId))
}


case class OpposingRecommendationsRepositoryLive(dataSource: DataSource) extends OpposingRecommendationsRepository {

  import civil.repositories.QuillContext._
  import civil.repositories.QuillContext.extras._

  override def insertOpposingRecommendation(opposingRec: OpposingRecommendations): ZIO[Any, AppError, Unit] = {
//    val recommendedContentIdIsDiscussion = opposingRec.recommendedContentId.map((recId) => {
//      val isDiscussion = run(query[Discussions].filter(st => st.id == lift(recId))).nonEmpty
//      isDiscussion
//    })
//    opposingRec.recommendedContentId.foreach(recId => {
//        val (topic, topicLink) = run(query[Spaces].filter(t => t.id == lift(opposingRec.targetContentId)).leftJoin(query[ExternalLinks]).on(_.id == _.topicId)).head
//        val (recSpace, recSpaceLink) = run(
//          query[Spaces].filter(t => t.id == lift(recId)).leftJoin(query[ExternalLinks]).on(_.id == _.topicId)
//      ).head
//        val fut = for {
//          recContentUrl <- recSpaceLink.map(data => data.externalContentUrl)
//          contentUrl <-  topicLink.map(data => data.externalContentUrl)
//          f = OutgoingHttp.sendHTTPToMLService("tfidf", UrlsForTFIDFConversion(contentUrl, recContentUrl))
//        } yield f
//        fut.foreach(f => {
//          f onComplete {
//            case Success(score) => {
//              val recToBeInserted = opposingRec.copy(isDiscussion = recommendedContentIdIsDiscussion.getOrElse(false), similarityScore = score.score)
//              run(query[OpposingRecommendations].insertValue(lift(recToBeInserted)))
//            }
//            case Failure(t) => println("An error has occurred: " + t.getMessage)
//          }
//        })
//    })
    ZIO.unit
  }

  override def getAllOpposingRecommendations(targetContentId: UUID): ZIO[Any, AppError, List[OutGoingOpposingRecommendations]] = {


    val q = quote {
      for {
        rec <- query[OpposingRecommendations].filter(rec => rec.targetContentId == lift(targetContentId)).sortBy(r => r.similarityScore)(Ord.descNullsLast)
        t <- query[Spaces].leftJoin(t => t.id === rec.recommendedContentId)
        st <- query[Discussions].leftJoin(st => st.id === rec.recommendedContentId)
      } yield (rec, t, st)
    }

    for  {
      outgoingRecs <- run(q).mapError(e => InternalServerError(e.toString)).provideEnvironment(ZEnvironment(dataSource))
      out = outgoingRecs.map { case (rec, topic, discussion) =>
        rec.into[OutGoingOpposingRecommendations]
          .withFieldConst(_.topic, topic)
          .withFieldConst(_.discussion, discussion)
          .withFieldConst(_.id, UUID.randomUUID())
          .transform
      }
    } yield out


  }

}

object OpposingRecommendationsRepositoryLive {
  val layer: URLayer[DataSource, OpposingRecommendationsRepository] = ZLayer.fromFunction(OpposingRecommendationsRepositoryLive.apply _)
}