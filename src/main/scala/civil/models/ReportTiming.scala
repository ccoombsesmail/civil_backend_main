package civil.models

import java.time.{LocalDateTime, ZonedDateTime}
import java.util.UUID



case class ReportTimings(contentId: UUID, reportPeriodEnd: Long, contentType: String, deletedAt: Option[ZonedDateTime] = None)
