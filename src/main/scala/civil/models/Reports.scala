package civil.models

import zio.json.{DeriveJsonCodec, JsonCodec}

import java.util.UUID
import java.time.{Instant, LocalDateTime, ZonedDateTime}

case class Report(
    contentId: UUID,
    toxic: Option[Boolean],
    spam: Option[Boolean],
    personalAttack: Option[Boolean],
    userId: Option[String] = None
)

object Report {
  implicit val codec: JsonCodec[Report] = DeriveJsonCodec.gen[Report]
}

case class Reports(
    userId: String,
    contentId: UUID,
    toxic: Option[Boolean],
    spam: Option[Boolean],
    personalAttack: Option[Boolean],
    contentType: String
)

case class ReportInfo(
    contentId: UUID,
    numToxicReports: Int,
    numPersonalAttackReports: Int,
    numSpamReports: Int,
    votedToAcquit: Option[Boolean],
    votedToStrike: Option[Boolean],
    reportPeriodEnd: Option[Long],
    votingEndedAt: Seq[Long] = Seq(),
    ongoing: Boolean,
    contentType: String,
    numVotesToAcquit: Option[Int] = None,
    numVotesToStrike: Option[Int] = None,
    numDefendantComments: Long = 0L,
    numReporterComments: Long = 0L,
    numJuryComments: Long = 0L,
    numGeneralComments: Long = 0L,
    numAllComments: Long = 0L
) {

  def attachVotingResults(
      votingPeriodEnd: Option[Long],
      numVotesToAcquit: Int,
      numVotesToStrike: Int
  ): ReportInfo = {
    if (
      votingPeriodEnd.isEmpty || votingPeriodEnd
        .exists(x => x < Instant.now().toEpochMilli)
    )
      this.copy(
        numVotesToAcquit = Some(numVotesToAcquit),
        numVotesToStrike = Some(numVotesToStrike)
      )
    else this
  }
}

object ReportInfo {
  implicit val codec: JsonCodec[ReportInfo] = DeriveJsonCodec.gen[ReportInfo]
}
