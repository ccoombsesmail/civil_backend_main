//package civil.services
//
//import civil.models.{ErrorInfo, IncomingPoll, Report, ReportInfo, Reports}
//import civil.repositories.ReportsRepository
//import io.scalaland.chimney.dsl.TransformerOps
//import zio.{Has, ZIO, ZLayer}
//
//import java.util.UUID
//
//trait PollsService {
//  def insertPoll(poll: IncomingPoll): ZIO[Any, ErrorInfo, Unit]
////  def getReport(jwt: String, jwtType: String, contentId: UUID): ZIO[Any, ErrorInfo, ReportInfo]
//
//}
//
//
//object PollsService {
//  def insertPoll(poll: IncomingPoll): ZIO[Has[PollsService], ErrorInfo, Unit] =
//    ZIO.serviceWith[PollsService](
//      _.insertPoll(poll)
//    )
//
////  def getReport(jwt: String, jwtType: String, contentId: UUID): ZIO[Has[ReportsService], ErrorInfo, ReportInfo] =
////    ZIO.serviceWith[ReportsService](
////      _.getReport(jwt, jwtType, contentId)
////    )
//}
//
//
//case class PollsServiceLive() extends PollsService {
//
//  override def insertReport(poll: IncomingPoll): ZIO[Any, ErrorInfo, Unit] = {
//    for {
//    } yield ()
//  }
//
////  override def getReport(jwt: String, jwtType: String, contentId: UUID): ZIO[Any, ErrorInfo, ReportInfo] = {
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
