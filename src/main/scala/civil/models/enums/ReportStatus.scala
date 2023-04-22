package civil.models.enums

import enumeratum._
import zio.json.{JsonDecoder, JsonEncoder}


sealed trait ReportStatus extends EnumEntry

case object ReportStatus extends Enum[ReportStatus] with CirceEnum[ReportStatus] with QuillEnum[ReportStatus] {

  case object Clean  extends ReportStatus
  case object Under_Review extends ReportStatus
  case object Removed  extends ReportStatus

  val values: IndexedSeq[ReportStatus] = findValues

  implicit val linkTypeEncoder: JsonEncoder[ReportStatus] = JsonEncoder[String].contramap(_.entryName)
  implicit val linkTypeDecoder: JsonDecoder[ReportStatus] = JsonDecoder[String].map {
    case "Clean" => Clean
    case "Under_Review" => Under_Review
    case "Removed" => Removed
  }

}

