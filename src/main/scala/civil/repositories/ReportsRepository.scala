package civil.repositories

import civil.errors.AppError
import civil.errors.AppError.InternalServerError
import civil.models.{ReportInfo, ReportTimings, Reports}
import civil.models._
import civil.models.NotifcationEvents._
import civil.models.enums.TribunalCommentType._
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

case class ReportsRepositoryLive(dataSource: DataSource)
    extends ReportsRepository {
  val runtime = zio.Runtime.default
  import civil.repositories.QuillContext._
  val kafka = new KafkaProducerServiceLive()

  private val REPORT_THRESHOLD = 2
  override def addReport(
      report: Reports
  ): ZIO[Any, AppError, Unit] = {
    (for {
      space <- run(
        query[Spaces].filter(t => t.id == lift(report.contentId))
      )
      discussion <- run(
        query[Discussions].filter(d => d.id == lift(report.contentId))
      )
      spaceOpt = space.headOption
      discussionOpt = discussion.headOption

      allReportsBefore <- run(
        query[Reports].filter(r => r.contentId == lift(report.contentId))
      )
        .mapError(e => {
          InternalServerError(e.getMessage)
        })
      contentType =
        if (spaceOpt.isDefined) "SPACE"
        else if (discussionOpt.isDefined) "DISCUSSION"
        else "COMMENT"
      reportWithContentType = report.copy(contentType = contentType)
      _ <- run(
        query[Reports].insertValue(lift(reportWithContentType))
      )
        .mapError(e => {
          InternalServerError(
            s"Sorry! There was an issue submitting the Report \n ${e.getMessage}"
          )
        })
      allReports <- run(
        query[Reports].filter(r => r.contentId == lift(report.contentId))
      )
        .mapError(e => {
          InternalServerError(e.getMessage)
        })
      _ <- ZIO.logInfo(
        s"All Report Before: ${allReportsBefore.length}. \n All Reports After: ${allReports.length}"
      )
      _ <- ZIO
        .when(
          allReports.length >= REPORT_THRESHOLD && allReportsBefore.length < REPORT_THRESHOLD
        ) {
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
        }
        .mapError(e => InternalServerError(e.toString))
    } yield ())
      .mapError(e => InternalServerError(e.toString))
      .provideEnvironment(ZEnvironment(dataSource))

  }

  override def getReport(
      contentId: UUID,
      userId: String
  ): ZIO[Any, AppError, ReportInfo] = {
    (for {
      votes <- run(
        query[TribunalVotes].filter(tv => tv.contentId == lift(contentId))
      )
        .mapError(e => InternalServerError(e.toString))
        .provideEnvironment(ZEnvironment(dataSource))

      numVotesToStrike = votes.count(tv => tv.voteToStrike.contains(true))
      numVotesToAcquit = votes.count(tv => tv.voteToAcquit.contains(true))

      voteOpt = votes.find(v => v.userId == userId)
      vote = voteOpt.getOrElse(
        TribunalVotes(
          userId = userId,
          contentId = contentId,
          voteToStrike = None,
          voteToAcquit = None
        )
      )
      timings <- run(
        query[ReportTimings].filter(rt => rt.contentId == lift(contentId))
      )
        .mapError(e => InternalServerError(e.toString))
        .provideEnvironment(ZEnvironment(dataSource))
      timing <- ZIO
        .fromOption(timings.headOption)
        .mapError(e => InternalServerError(e.toString))
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

      groupedTComments <- run(
        query[TribunalComments]
          .filter(tc => tc.reportedContentId == lift(contentId))
          .groupBy(_.commentType)
          .map { case (commentType, comments) =>
            (commentType, comments.size)
          }
      ).mapError(e => InternalServerError(e.toString))
      commentTypeMap = groupedTComments.toMap
      numDefendantComments = commentTypeMap.getOrElse(Defendant, 0L)
      numJuryComments = commentTypeMap.getOrElse(Jury, 0L)
      numGeneralComments = commentTypeMap.getOrElse(General, 0L)
      numReporterComments = commentTypeMap.getOrElse(Reporter, 0L)
      numAllComments =
        numGeneralComments + numReporterComments + numJuryComments + numDefendantComments
    } yield ReportInfo(
      contentId,
      reportsMap.getOrElse("toxic", 0),
      reportsMap.getOrElse("personalAttack", 0),
      reportsMap.getOrElse("spam", 0),
      votedToAcquit = vote.voteToAcquit,
      votedToStrike = vote.voteToStrike,
      Some(timing.reportPeriodEnd),
      timing.reviewEndingTimes,
      contentType = timing.contentType,
      ongoing = timing.ongoing,
      numDefendantComments = numDefendantComments,
      numJuryComments = numJuryComments,
      numGeneralComments = numGeneralComments,
      numReporterComments = numReporterComments,
      numAllComments = numAllComments
    ).attachVotingResults(
      Some(timing.reportPeriodEnd),
      numVotesToStrike = numVotesToStrike,
      numVotesToAcquit = numVotesToAcquit
    )).provideEnvironment(ZEnvironment(dataSource))
  }
}

object ReportsRepositoryLive {
  val layer: URLayer[DataSource, ReportsRepository] =
    ZLayer.fromFunction(ReportsRepositoryLive.apply _)
}
