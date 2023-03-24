package civil.models

import java.util.UUID

case class CommentReport(
    commentId: UUID,
    toxic: Option[Boolean],
    spam: Option[Boolean],
    personalAttack: Option[Boolean]
)

case class CommentReports(
    userId: String,
    commentId: UUID,
    toxic: Option[Boolean],
    spam: Option[Boolean],
    personalAttack: Option[Boolean]
)

case class CommentReportInfo(
    commentId: UUID,
    numToxicReports: Int,
    numPersonalAttackReports: Int,
    numSpamReports: Int,
    voteAgainst: Option[Boolean],
    voteFor: Option[Boolean],
    reportPeriodEnd: Long
)
