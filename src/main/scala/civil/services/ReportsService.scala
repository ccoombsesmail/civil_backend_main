package civil.services

import civil.models.{ErrorInfo, Report, ReportInfo, Reports}
import civil.repositories.ReportsRepository
import io.scalaland.chimney.dsl.TransformerOps
import zio.{Has, ZIO, ZLayer}

import java.util.UUID

trait ReportsService {
  def addReport(jwt: String, jwtType: String, report: Report): ZIO[Any, ErrorInfo, Unit]
  def getReport(jwt: String, jwtType: String, contentId: UUID): ZIO[Any, ErrorInfo, ReportInfo]

}


object ReportsService {
  def addReport(jwt: String, jwtType: String, report: Report): ZIO[Has[ReportsService], ErrorInfo, Unit] =
    ZIO.serviceWith[ReportsService](
      _.addReport(jwt, jwtType, report)
    )

  def getReport(jwt: String, jwtType: String, contentId: UUID): ZIO[Has[ReportsService], ErrorInfo, ReportInfo] =
    ZIO.serviceWith[ReportsService](
      _.getReport(jwt, jwtType, contentId)
    )
}


case class ReportsServiceLive(reportsRepo: ReportsRepository) extends ReportsService {

  override def addReport(jwt: String, jwtType: String, report: Report): ZIO[Any, ErrorInfo, Unit] = {
    val authenticationService = AuthenticationServiceLive()
    for {
      userData <- authenticationService.extractUserData(jwt, jwtType)
      _ <- reportsRepo.addReport(
        report
          .into[Reports]
          .withFieldConst(_.userId, userData.userId)
          .withFieldConst(_.contentType, "Dummy")
          .transform
      )
    } yield ()
  }

  override def getReport(jwt: String, jwtType: String, contentId: UUID): ZIO[Any, ErrorInfo, ReportInfo] = {
    val authenticationService = AuthenticationServiceLive()

    for {
      userData <- authenticationService.extractUserData(jwt, jwtType)
      reportInfo <- reportsRepo.getReport(contentId, userData.userId)
    } yield reportInfo
  }
}



object ReportsServiceLive {
  val live: ZLayer[Has[ReportsRepository], Nothing, Has[
    ReportsService
  ]] = {
    for {
      reportsRepo <- ZIO.service[ReportsRepository]
    } yield ReportsServiceLive(reportsRepo)
  }.toLayer
}
