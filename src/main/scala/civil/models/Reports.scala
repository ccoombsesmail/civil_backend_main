package civil.models

import zio.json.{DeriveJsonCodec, JsonCodec}

import java.util.UUID
import java.time.{Instant, LocalDateTime}

case class Report(
    contentId: UUID,
    toxic: Option[Boolean],
    spam: Option[Boolean],
    personalAttack: Option[Boolean]
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
                       voteAgainst: Option[Boolean],
                       voteFor: Option[Boolean],
                       reportPeriodEnd: Option[Long],
                       votingEndedAt: Option[LocalDateTime],
                       contentType: String,
                       numVotesAgainst: Option[Int] = None,
                       numVotesFor: Option[Int] = None
) {

  def attachVotingResults(votingPeriodEnd: Option[Long], votesFor: Int, votesAgainst: Int): ReportInfo = {
    if (votingPeriodEnd.isEmpty || votingPeriodEnd.exists(x => x < Instant.now().toEpochMilli)) this.copy(numVotesAgainst = Some(votesAgainst), numVotesFor = Some(votesFor))
    else this
  }
}

object ReportInfo {
  implicit val codec: JsonCodec[ReportInfo] = DeriveJsonCodec.gen[ReportInfo]
}