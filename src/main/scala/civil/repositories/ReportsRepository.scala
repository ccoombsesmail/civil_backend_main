package civil.repositories

import civil.errors.AppError
import civil.errors.AppError.InternalServerError
import civil.models.{
  ReportInfo,
  ReportTimings,
  Reports,
  Topics
}
import civil.models._
import civil.models.NotifcationEvents._
import zio._

import java.util.UUID
import civil.services.KafkaProducerServiceLive

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

case class ReportsRepositoryLive() extends ReportsRepository {
  val runtime = zio.Runtime.default
  import QuillContextHelper.ctx._
  val kafka = new KafkaProducerServiceLive()

  val REPORT_THRESHOLD = 1
  override def addReport(
      report: Reports
  ): ZIO[Any, AppError, Unit] = {
    for {
      topicOpt <- ZIO
        .attempt(
          run(
            query[Topics].filter(t => t.id == lift(report.contentId))
          ).headOption
        )
        .mapError(e => InternalServerError(e.toString))
      contentType = if (topicOpt.isDefined) "TOPIC" else "COMMENT"
      reportWithContentType = report.copy(contentType = contentType)
      _ <- ZIO
        .attempt(
          run(
            query[Reports].insert(lift(reportWithContentType))
          )
        )
        .mapError(e => {
          InternalServerError(s"Sorry! There was an issue saving the Report \n ${e.getMessage}")
        })
      allReports <- ZIO
        .attempt(
          run(
            query[Reports].filter(r => r.contentId == lift(report.contentId))
          )
        )
        .mapError(e => {
          println(e)
          InternalServerError(s"Error Getting All Reports")
        })
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
      votes <- ZIO
        .attempt(
          run(
            query[TribunalVotes].filter(tv => tv.contentId == lift(contentId))
          )
        )
        .mapError(e => InternalServerError(e.toString))

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
      timing <- ZIO
        .attempt(
          run(
            query[ReportTimings].filter(rt => rt.contentId == lift(contentId))
          ).headOption
        )
        .mapError(e => InternalServerError(e.toString))
      reports <- ZIO
        .attempt(
          run(
            query[Reports].filter(tr => tr.contentId == lift(contentId))
          )
        )
        .mapError(e => InternalServerError(e.toString))
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
      timing.map(_.reportPeriodEnd),
      timing.flatMap(_.deletedAt),
      contentType = "TOPIC"
    ).attachVotingResults(timing.map(_.reportPeriodEnd), numVotesAgainst, numVotesFor)
  }
}

object ReportsRepositoryLive {
  val layer: URLayer[Any, ReportsRepository] = ZLayer.fromFunction(ReportsRepositoryLive.apply _)
}
