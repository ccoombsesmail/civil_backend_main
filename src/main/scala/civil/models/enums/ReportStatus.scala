package civil.models.enums

import enumeratum._
import zio.json.{JsonDecoder, JsonEncoder}

sealed trait ReportStatus extends EnumEntry

case object ReportStatus
    extends Enum[ReportStatus]
    with CirceEnum[ReportStatus]
    with QuillEnum[ReportStatus] {

  case object CLEAN extends ReportStatus
  case object UNDER_REVIEW extends ReportStatus
  case object REMOVED extends ReportStatus
  case object MARKED extends ReportStatus

  val values: IndexedSeq[ReportStatus] = findValues

  implicit val linkTypeEncoder: JsonEncoder[ReportStatus] =
    JsonEncoder[String].contramap(_.entryName)
  implicit val linkTypeDecoder: JsonDecoder[ReportStatus] =
    JsonDecoder[String].map {
      case "CLEAN"        => CLEAN
      case "UNDER_REVIEW" => UNDER_REVIEW
      case "REMOVED"      => REMOVED
      case "MARKED"       => MARKED
    }

}
