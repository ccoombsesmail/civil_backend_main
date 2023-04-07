package civil.services

import civil.models.{OpposingRecommendations, OutGoingOpposingRecommendations}
import java.util.UUID
import civil.errors.AppError
import civil.repositories.recommendations.OpposingRecommendationsRepository
import zio._

trait OpposingRecommendationsService {
  def insertOpposingRecommendation(opposingRec: OpposingRecommendations): ZIO[Any, AppError, Unit]
  def getAllOpposingRecommendations(targetContentId: UUID): ZIO[Any, AppError, List[OutGoingOpposingRecommendations]]
}

object OpposingRecommendationsService {
  def insertOpposingRecommendation(opposingRec: OpposingRecommendations): ZIO[OpposingRecommendationsService, AppError, Unit] =
    ZIO.serviceWithZIO[OpposingRecommendationsService](_.insertOpposingRecommendation(opposingRec))
  def getAllOpposingRecommendations(targetContentId: UUID): ZIO[OpposingRecommendationsService, AppError, List[OutGoingOpposingRecommendations]] =
    ZIO.serviceWithZIO[OpposingRecommendationsService](_.getAllOpposingRecommendations(targetContentId))
}


case class OpposingRecommendationsServiceLive(opposingRecommendationsRepo: OpposingRecommendationsRepository) extends OpposingRecommendationsService {

  override def insertOpposingRecommendation(opposingRec: OpposingRecommendations): ZIO[Any, AppError, Unit] = {
    opposingRecommendationsRepo.insertOpposingRecommendation(opposingRec)
  }

  override def getAllOpposingRecommendations(targetContentId: UUID): ZIO[Any, AppError, List[OutGoingOpposingRecommendations]] = {
    opposingRecommendationsRepo.getAllOpposingRecommendations(targetContentId)
  }

}


object OpposingRecommendationsServiceLive {
  val layer: URLayer[OpposingRecommendationsRepository, OpposingRecommendationsService] = ZLayer.fromFunction(OpposingRecommendationsServiceLive.apply _)

}

