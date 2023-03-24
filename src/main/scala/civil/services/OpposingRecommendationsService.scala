package civil.services

import civil.models.{ErrorInfo, OpposingRecommendations, OutGoingOpposingRecommendations}

import java.util.UUID
import civil.models.ErrorInfo
import civil.repositories.recommendations.OpposingRecommendationsRepository
import zio._

trait OpposingRecommendationsService {
  def insertOpposingRecommendation(opposingRec: OpposingRecommendations): ZIO[Any, ErrorInfo, Unit]
  def getAllOpposingRecommendations(targetContentId: UUID): ZIO[Any, ErrorInfo, List[OutGoingOpposingRecommendations]]
}

object OpposingRecommendationsService {
  def insertOpposingRecommendation(opposingRec: OpposingRecommendations): ZIO[Has[OpposingRecommendationsService], ErrorInfo, Unit] =
    ZIO.serviceWith[OpposingRecommendationsService](_.insertOpposingRecommendation(opposingRec))
  def getAllOpposingRecommendations(targetContentId: UUID): ZIO[Has[OpposingRecommendationsService], ErrorInfo, List[OutGoingOpposingRecommendations]] =
    ZIO.serviceWith[OpposingRecommendationsService](_.getAllOpposingRecommendations(targetContentId))
}


case class OpposingRecommendationsServiceLive(opposingRecommendationsRepo: OpposingRecommendationsRepository) extends OpposingRecommendationsService {

  override def insertOpposingRecommendation(opposingRec: OpposingRecommendations): ZIO[Any, ErrorInfo, Unit] = {
    opposingRecommendationsRepo.insertOpposingRecommendation(opposingRec)
  }

  override def getAllOpposingRecommendations(targetContentId: UUID): ZIO[Any, ErrorInfo, List[OutGoingOpposingRecommendations]] = {
    opposingRecommendationsRepo.getAllOpposingRecommendations(targetContentId)
  }

}


object OpposingRecommendationsServiceLive {
  val live: ZLayer[Has[OpposingRecommendationsRepository], Throwable, Has[OpposingRecommendationsService]] = {
    for {
      opposingRecommendationsRepo <- ZIO.service[OpposingRecommendationsRepository]
    } yield OpposingRecommendationsServiceLive(opposingRecommendationsRepo)
  }.toLayer
}

