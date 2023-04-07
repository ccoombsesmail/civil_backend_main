//package civil.services
//
//import civil.models.{AppError, IncomingPoll, Report, ReportInfo, Reports}
//import civil.repositories.ReportsRepository
//import io.scalaland.chimney.dsl.TransformerOps
//import zio.{Has, ZIO, ZLayer}
//
//import java.util.UUID
//
//trait PollsService {
//  def insertPoll(poll: IncomingPoll): ZIO[Any, AppError, Unit]
////  def getReport(jwt: String, jwtType: String, contentId: UUID): ZIO[Any, AppError, ReportInfo]
//
//}
//
//
//object PollsService {
//  def insertPoll(poll: IncomingPoll): ZIO[Has[PollsService], AppError, Unit] =
//    ZIO.serviceWith[PollsService](
//      _.insertPoll(poll)
//    )
//
////  def getReport(jwt: String, jwtType: String, contentId: UUID): ZIO[Has[ReportsService], AppError, ReportInfo] =
////    ZIO.serviceWith[ReportsService](
////      _.getReport(jwt, jwtType, contentId)
////    )
//}
//
//
//case class PollsServiceLive() extends PollsService {
//
//  override def insertReport(poll: IncomingPoll): ZIO[Any, AppError, Unit] = {
//    for {
//    } yield ()
//  }
//
////  override def getReport(jwt: String, jwtType: String, contentId: UUID): ZIO[Any, AppError, ReportInfo] = {
////    val authenticationService = AuthenticationServiceLive()
////
////    for {
////      userData <- authenticationService.extractUserData(jwt, jwtType)
////      reportInfo <- reportsRepo.getReport(contentId, userData.userId)
////    } yield reportInfo
////  }
//}
//
//
//
//object ReportsServiceLive {
//  val live: ZLayer[Has[ReportsRepository], Nothing, Has[
//    ReportsService
//  ]] = {
//    for {
//      reportsRepo <- ZIO.service[ReportsRepository]
//    } yield ReportsServiceLive(reportsRepo)
//  }.toLayer
//}
