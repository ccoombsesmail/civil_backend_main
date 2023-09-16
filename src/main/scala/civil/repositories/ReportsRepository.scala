package civil.repositories

import civil.errors.AppError
import civil.errors.AppError.DatabaseError
import civil.models.{ReportInfo, ReportTimings, Reports}
import civil.models._
import civil.models.NotifcationEvents._
import civil.models.enums.ReportCause
import civil.models.enums.TribunalCommentType._
import civil.repositories.ReportQueries.{
  getReportCountsByCause,
  getReportCountsBySeverity
}
import zio._

import java.util.UUID
import civil.services.KafkaProducerServiceLive

import javax.sql.DataSource

trait ReportsRepository {
  def addReport(report: Reports): ZIO[Any, AppError, Unit]

  def getReport(contentId: UUID, userId: String): ZIO[Any, AppError, ReportInfo]

  def getReportUnauthenticated(contentId: UUID): ZIO[Any, AppError, ReportInfo]
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

  def getReportUnauthenticated(
      contentId: UUID
  ): ZIO[ReportsRepository, AppError, ReportInfo] =
    ZIO.serviceWithZIO[ReportsRepository](
      _.getReportUnauthenticated(contentId)
    )
}

case class ReportsRepositoryLive(dataSource: DataSource)
    extends ReportsRepository {
  val runtime = zio.Runtime.default

  import civil.repositories.QuillContext._

  val kafka = new KafkaProducerServiceLive()

  private val REPORT_THRESHOLD: Int = 1

  override def addReport(report: Reports): ZIO[Any, AppError, Unit] = {
    (for {
      // Get the number of reports by severity before the new report
      reportsBySeverityBefore <- getReportCountsBySeverity(report.contentId)

      reportsBySeverityBeforeMap = reportsBySeverityBefore.toMap
      // Insert the new report
      _ <- run(
        query[Reports].insertValue(lift(report))
      )
        .mapError(e =>
          DatabaseError(
            new Throwable(
              s"There was an issue submitting the Report -> ${report} \n ${e.getMessage}"
            )
          )
        )

      // Get the number of reports by severity after the new report
      reportsBySeverityAfter <- getReportCountsBySeverity(report.contentId)

      reportsBySeverityAfterMap = reportsBySeverityAfter.toMap

      // Check if any severity category has just surpassed the VOTE_THRESHOLD
      _ <- ZIO.foreach(reportsBySeverityAfterMap) {
        case (severity, countAfter) =>
          val countBefore: Long =
            reportsBySeverityBeforeMap.getOrElse(severity, 0)
          ZIO
            .when(
              countAfter.toInt >= REPORT_THRESHOLD && countBefore.toInt < REPORT_THRESHOLD
            ) {
              kafka.publish(
                ContentReported(
                  eventType = "ContentReported",
                  contentType = report.contentType,
                  reportedContentId = report.contentId,
                  severity = report.severity
                ),
                report.contentId.toString,
                ContentReported.contentReportedSerde,
                topic = "reports"
              )
            }
            .as(("", 0))
      }
    } yield ())
      .mapError(e => DatabaseError(e))
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
        .mapError(DatabaseError(_))

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
      timingQueryResult <- run(
        query[ReportTimings].filter(rt => rt.contentId == lift(contentId))
      )
        .mapError(DatabaseError(_))
      timing <- ZIO
        .fromOption(timingQueryResult.headOption)
        .orElseFail(DatabaseError(new Throwable("sdf")))

      reportsByCauseQueryResult <- getReportCountsByCause(contentId)

      reportsByCauseMap = reportsByCauseQueryResult.toMap
      reportsByCauseUnderReviewMap = reportsByCauseMap.filter {
        case (cause, _) =>
          ReportCause.getSeverity(
            ReportCause.withName(cause)
          ) == timing.severity
      }
      reportsByCauseNotUnderReviewMap = reportsByCauseMap.filter {
        case (cause, _) =>
          ReportCause.getSeverity(
            ReportCause.withName(cause)
          ) != timing.severity
      }

      groupedTComments <- run(
        query[TribunalComments]
          .filter(tc => tc.reportedContentId == lift(contentId))
          .groupBy(_.commentType)
          .map { case (commentType, comments) =>
            (commentType, comments.size)
          }
      ).mapError(DatabaseError)
      commentTypeMap = groupedTComments.toMap
      numDefendantComments = commentTypeMap.getOrElse(Defendant, 0L)
      numJuryComments = commentTypeMap.getOrElse(Jury, 0L)
      numGeneralComments = commentTypeMap.getOrElse(General, 0L)
      numReporterComments = commentTypeMap.getOrElse(Reporter, 0L)
      numAllComments =
        numGeneralComments + numReporterComments + numJuryComments + numDefendantComments
    } yield ReportInfo(
      contentId,
      reportsByCauseUnderReviewMap = reportsByCauseUnderReviewMap,
      reportsByCauseNotUnderReviewMap = reportsByCauseNotUnderReviewMap,
      votedToAcquit = vote.voteToAcquit,
      votedToStrike = vote.voteToStrike,
      reportPeriodEnd = Some(timing.reportPeriodEnd),
      votingEndedAt = timing.reviewEndingTimes,
      contentType = timing.contentType,
      ongoing = timing.ongoing,
      numDefendantComments = numDefendantComments,
      numJuryComments = numJuryComments,
      numGeneralComments = numGeneralComments,
      numReporterComments = numReporterComments,
      numAllComments = numAllComments,
      reportSeverityLevel = timing.severity
    ).attachVotingResults(
      Some(timing.reportPeriodEnd),
      numVotesToStrike = numVotesToStrike,
      numVotesToAcquit = numVotesToAcquit
    ))
      .mapError(DatabaseError)
      .provideEnvironment(ZEnvironment(dataSource))
  }

  override def getReportUnauthenticated(
      contentId: UUID
  ): ZIO[Any, AppError, ReportInfo] = {
    (for {

      timingQueryResult <- run(
        query[ReportTimings].filter(rt => rt.contentId == lift(contentId))
      )
        .mapError(e =>
          DatabaseError(
            new Throwable(
              s"Error querying for report timing for contentId=${contentId} with error $e"
            )
          )
        )
      timing <- ZIO
        .fromOption(timingQueryResult.headOption)
        .orElseFail(
          DatabaseError(
            new Throwable(
              s"Report timing for contentId=${contentId} does not exist"
            )
          )
        )

      reportsByCauseQueryResult <- getReportCountsByCause(contentId)

      reportsByCauseMap = reportsByCauseQueryResult.toMap
      reportsByCauseUnderReviewMap = reportsByCauseMap.filter {
        case (cause, _) =>
          ReportCause.getSeverity(
            ReportCause.withName(cause)
          ) == timing.severity
      }
      reportsByCauseNotUnderReviewMap = reportsByCauseMap.filter {
        case (cause, _) =>
          ReportCause.getSeverity(
            ReportCause.withName(cause)
          ) != timing.severity
      }

      groupedTComments <- run(
        query[TribunalComments]
          .filter(tc => tc.reportedContentId == lift(contentId))
          .groupBy(_.commentType)
          .map { case (commentType, comments) =>
            (commentType, comments.size)
          }
      ).mapError(e =>
        DatabaseError(
          new Throwable(
            s"Error querying for the number of tribunal comments of eahc type for content with contentId=${contentId} with error $e"
          )
        )
      )

      commentTypeMap = groupedTComments.toMap
      numDefendantComments = commentTypeMap.getOrElse(Defendant, 0L)
      numJuryComments = commentTypeMap.getOrElse(Jury, 0L)
      numGeneralComments = commentTypeMap.getOrElse(General, 0L)
      numReporterComments = commentTypeMap.getOrElse(Reporter, 0L)
      numAllComments =
        numGeneralComments + numReporterComments + numJuryComments + numDefendantComments
    } yield ReportInfo(
      contentId,
      reportsByCauseUnderReviewMap = reportsByCauseUnderReviewMap,
      reportsByCauseNotUnderReviewMap = reportsByCauseNotUnderReviewMap,
      votedToAcquit = Some(false),
      votedToStrike = Some(false),
      reportPeriodEnd = Some(timing.reportPeriodEnd),
      votingEndedAt = timing.reviewEndingTimes,
      contentType = timing.contentType,
      ongoing = timing.ongoing,
      numDefendantComments = numDefendantComments,
      numJuryComments = numJuryComments,
      numGeneralComments = numGeneralComments,
      numReporterComments = numReporterComments,
      numAllComments = numAllComments,
      reportSeverityLevel = timing.severity
    ).attachVotingResults(
      Some(timing.reportPeriodEnd),
      numVotesToStrike = 0,
      numVotesToAcquit = 0
    ))
      .mapError(DatabaseError)
      .provideEnvironment(ZEnvironment(dataSource))
  }
}

object ReportsRepositoryLive {
  val layer: URLayer[DataSource, ReportsRepository] =
    ZLayer.fromFunction(ReportsRepositoryLive.apply _)
}
