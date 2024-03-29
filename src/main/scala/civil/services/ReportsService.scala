package civil.services

import civil.errors.AppError
import civil.models.enums.ReportCause
import civil.models.{Report, ReportInfo, Reports}
import civil.repositories.ReportsRepository
import io.scalaland.chimney.dsl.TransformerOps
import zio.{URLayer, ZIO, ZLayer}

import java.util.UUID

trait ReportsService {
  def addReport(
      jwt: String,
      jwtType: String,
      report: Report
  ): ZIO[Any, AppError, Unit]

  def getReport(
      jwt: String,
      jwtType: String,
      contentId: UUID
  ): ZIO[Any, AppError, ReportInfo]

  def getReportUnauthenticated(contentId: UUID): ZIO[Any, AppError, ReportInfo]
}

object ReportsService {
  def addReport(
      jwt: String,
      jwtType: String,
      report: Report
  ): ZIO[ReportsService, AppError, Unit] =
    ZIO.serviceWithZIO[ReportsService](
      _.addReport(jwt, jwtType, report)
    )

  def getReport(
      jwt: String,
      jwtType: String,
      contentId: UUID
  ): ZIO[ReportsService, AppError, ReportInfo] =
    ZIO.serviceWithZIO[ReportsService](
      _.getReport(jwt, jwtType, contentId)
    )

  def getReportUnauthenticated(
      contentId: UUID
  ): ZIO[ReportsService, AppError, ReportInfo] =
    ZIO.serviceWithZIO[ReportsService](
      _.getReportUnauthenticated(contentId)
    )
}

case class ReportsServiceLive(
    reportsRepo: ReportsRepository,
    authenticationService: AuthenticationService
) extends ReportsService {

  override def addReport(
      jwt: String,
      jwtType: String,
      report: Report
  ): ZIO[Any, AppError, Unit] = {
    for {
      userData <- authenticationService.extractUserData(jwt, jwtType)
      _ <- reportsRepo.addReport(
        report
          .into[Reports]
          .withFieldConst(_.userId, userData.userId)
          .withFieldComputed(_.reportCause, r => r.reportCause.entryName)
          .withFieldComputed(
            _.severity,
            r => ReportCause.getSeverity(r.reportCause)
          )
          .transform
      )
    } yield ()
  }

  override def getReport(
      jwt: String,
      jwtType: String,
      contentId: UUID
  ): ZIO[Any, AppError, ReportInfo] = {
    for {
      userData <- authenticationService.extractUserData(jwt, jwtType)
      reportInfo <- reportsRepo.getReport(contentId, userData.userId)
    } yield reportInfo
  }

  override def getReportUnauthenticated(
      contentId: UUID
  ): ZIO[Any, AppError, ReportInfo] = {
    reportsRepo.getReportUnauthenticated(contentId)
  }
}

object ReportsServiceLive {
  val layer
      : URLayer[ReportsRepository with AuthenticationService, ReportsService] =
    ZLayer.fromFunction(ReportsServiceLive.apply _)
}
