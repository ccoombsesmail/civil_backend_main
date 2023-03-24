package civil.services

import civil.models.{ErrorInfo, OutgoingRecommendations}

import java.util.UUID
import civil.models.ErrorInfo
import civil.repositories.recommendations.RecommendationsRepository
import zio._

trait RecommendationsService {
  def getAllRecommendations(targetContentId: UUID): ZIO[Any, ErrorInfo, List[OutgoingRecommendations]]
}

object RecommendationsService {
  def getAllRecommendations(targetContentId: UUID): ZIO[Has[RecommendationsService], ErrorInfo, List[OutgoingRecommendations]] =
    ZIO.serviceWith[RecommendationsService](_.getAllRecommendations(targetContentId))
}


case class RecommendationsServiceLive(recommendationsRepo: RecommendationsRepository) extends RecommendationsService {

  override def getAllRecommendations(targetContentId: UUID): ZIO[Any, ErrorInfo, List[OutgoingRecommendations]] = {
    recommendationsRepo.getAllRecommendations(targetContentId)
  }

}


object RecommendationsServiceLive {
  val live: ZLayer[Has[RecommendationsRepository], Throwable, Has[RecommendationsService]] = {
    for {
      recommendationsRepo <- ZIO.service[RecommendationsRepository]
    } yield RecommendationsServiceLive(recommendationsRepo)
  }.toLayer
}

