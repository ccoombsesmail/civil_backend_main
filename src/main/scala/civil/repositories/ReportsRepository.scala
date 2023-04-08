package civil.repositories

import civil.errors.AppError
import civil.errors.AppError.InternalServerError
import civil.models.{ReportInfo, ReportTimings, Reports, Topics}
import civil.models._
import civil.models.NotifcationEvents._
import zio._

import java.util.UUID
import civil.services.KafkaProducerServiceLive

import javax.sql.DataSource

trait ReportsRepository {
  def addReport(report: Reports): ZIO[Any, AppError, Unit]
  def getReport(
      contentId: UUID,
      userId: String
  ): ZIO[Any, AppError, ReportInfo]

}

object ReportsRepository {
  def addReport(
      report: Reports
  ): ZIO[ReportsRepository, AppError, Unit] =
    ZIO.serviceWithZIO[ReportsRepository](
      _.addReport(report)
    )

  def getReport(
      contentId: UUID,
      userId: String
  ): ZIO[ReportsRepository, AppError, ReportInfo] =
    ZIO.serviceWithZIO[ReportsRepository](
      _.getReport(contentId, userId)
    )
}

case class ReportsRepositoryLive(dataSource: DataSource) extends ReportsRepository {
  val runtime = zio.Runtime.default
  import civil.repositories.QuillContext._
  val kafka = new KafkaProducerServiceLive()

  val REPORT_THRESHOLD = 1
  override def addReport(
      report: Reports
  ): ZIO[Any, AppError, Unit] = {
    for {
      topics <- run(
            query[Topics].filter(t => t.id == lift(report.contentId))
          ).mapError(e => InternalServerError(e.toString)).provideEnvironment(ZEnvironment(dataSource))
      topicOpt = topics.headOption
      contentType = if (topicOpt.isDefined) "TOPIC" else "COMMENT"
      reportWithContentType = report.copy(contentType = contentType)
      _ <- run(
            query[Reports].insertValue(lift(reportWithContentType))
          )
        .mapError(e => {
          InternalServerError(s"Sorry! There was an issue saving the Report \n ${e.getMessage}")
        }).provideEnvironment(ZEnvironment(dataSource))
      allReports <- run(
            query[Reports].filter(r => r.contentId == lift(report.contentId))
          )
        .mapError(e => {
          InternalServerError(e.getMessage)
        }).provideEnvironment(ZEnvironment(dataSource))
      _ <- ZIO.when(allReports.length >= REPORT_THRESHOLD) {
        ZIO.attempt(
          kafka.publish(
            ContentReported(
              eventType = "ContentReported",
              contentType = contentType,
              reportedContentId = report.contentId
            ),
            report.contentId.toString,
            ContentReported.contentReportedSerde,
            topic = "reports"
          )
        )
      }.mapError(e => InternalServerError(e.toString))
    } yield ()

  }

  override def getReport(
      contentId: UUID,
      userId: String
  ): ZIO[Any, AppError, ReportInfo] = {
    for {
      votes <- run(
            query[TribunalVotes].filter(tv => tv.contentId == lift(contentId))
          )
        .mapError(e => InternalServerError(e.toString))
        .provideEnvironment(ZEnvironment(dataSource))

      numVotesFor = votes.count(tv => tv.voteFor.contains(true))
      numVotesAgainst = votes.count(tv => tv.voteAgainst.contains(true))

      voteOpt = votes.find(v => v.userId == userId)
      vote = voteOpt.getOrElse(
        TribunalVotes(
          userId = userId,
          contentId = contentId,
          voteAgainst = None,
          voteFor = None
        )
      )
      timings <- run(
            query[ReportTimings].filter(rt => rt.contentId == lift(contentId))
          )
        .mapError(e => InternalServerError(e.toString))
        .provideEnvironment(ZEnvironment(dataSource))
      timingOpt = timings.headOption
      reports <- run(
            query[Reports].filter(tr => tr.contentId == lift(contentId))
          )
        .mapError(e => InternalServerError(e.toString))
        .provideEnvironment(ZEnvironment(dataSource))
      reportsMap = reports.foldLeft(Map[String, Int]()) { (m, t) =>
        val m1 = if (t.toxic.isDefined) {
          if (m.contains("toxic"))
            m + ("toxic" -> (m.getOrElse("toxic", 0) + 1))
          else m + ("toxic" -> 1)
        } else m

        val m2 = if (t.personalAttack.isDefined) {
          if (m1.contains("personalAttack"))
            m1 + ("personalAttack" -> (m1.getOrElse("personalAttack", 0) + 1))
          else m1 + ("personalAttack" -> 1)
        } else m1

        val m3 = if (t.spam.isDefined) {
          if (m2.contains("spam"))
            m2 + ("spam" -> (m2.getOrElse("spam", 0) + 1))
          else m2 + ("spam" -> 1)
        } else m2
        m3
      }
    } yield ReportInfo(
      contentId,
      reportsMap.getOrElse("toxic", 0),
      reportsMap.getOrElse("personalAttack", 0),
      reportsMap.getOrElse("spam", 0),
      vote.voteAgainst,
      vote.voteFor,
      timingOpt.map(_.reportPeriodEnd),
      timingOpt.flatMap(_.deletedAt),
      contentType = "TOPIC"
    ).attachVotingResults(timingOpt.map(_.reportPeriodEnd), numVotesAgainst, numVotesFor)
  }
}

object ReportsRepositoryLive {
  val layer: URLayer[DataSource, ReportsRepository] = ZLayer.fromFunction(ReportsRepositoryLive.apply _)
}
