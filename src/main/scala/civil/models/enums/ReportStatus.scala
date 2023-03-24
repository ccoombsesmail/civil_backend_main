package civil.models.enums

import enumeratum._


sealed trait ReportStatus extends EnumEntry

case object ReportStatus extends Enum[ReportStatus] with CirceEnum[ReportStatus] with QuillEnum[ReportStatus] {

  case object Clean  extends ReportStatus
  case object Under_Review extends ReportStatus
  case object Removed  extends ReportStatus

  val values: IndexedSeq[ReportStatus] = findValues

}

