package civil.services

import civil.controllers.RecommendationsController
import civil.models.OutgoingRecommendations

import java.util.UUID
import civil.errors.AppError
import civil.repositories.recommendations.RecommendationsRepository
import zio._

trait RecommendationsService {
  def getAllRecommendations(targetContentId: UUID): ZIO[Any, AppError, List[OutgoingRecommendations]]
}

object RecommendationsService {
  def getAllRecommendations(targetContentId: UUID): ZIO[RecommendationsService, AppError, List[OutgoingRecommendations]] =
    ZIO.serviceWithZIO[RecommendationsService](_.getAllRecommendations(targetContentId))
}


case class RecommendationsServiceLive(recommendationsRepo: RecommendationsRepository) extends RecommendationsService {

  override def getAllRecommendations(targetContentId: UUID): ZIO[Any, AppError, List[OutgoingRecommendations]] = {
    recommendationsRepo.getAllRecommendations(targetContentId)
  }

}


object RecommendationsServiceLive {
  val layer: URLayer[RecommendationsRepository, RecommendationsService] = ZLayer.fromFunction(RecommendationsServiceLive.apply _)
}

