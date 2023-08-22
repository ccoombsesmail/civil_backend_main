package civil.models.enums

import enumeratum._
import zio.json.{DeriveJsonCodec, JsonCodec, JsonDecoder, JsonEncoder}

sealed trait ReportSeverity extends EnumEntry

case object ReportSeverity extends Enum[ReportSeverity] {
  case object Critical extends ReportSeverity
  case object Severe extends ReportSeverity
  case object Moderate extends ReportSeverity
  case object Low extends ReportSeverity

  val values: IndexedSeq[ReportSeverity] = findValues
}
sealed trait ReportCause extends EnumEntry

case object ReportCause
    extends Enum[ReportCause]
    with CirceEnum[ReportCause]
    with QuillEnum[ReportCause] {

  case object Targeted extends ReportCause
  case object Dox extends ReportCause
  case object Threat extends ReportCause
  case object Explicit extends ReportCause
  case object GraphicViolence extends ReportCause
  case object Terrorism extends ReportCause
  case object Profanity extends ReportCause
  case object Impersonation extends ReportCause
  case object Racism extends ReportCause

  val values: IndexedSeq[ReportCause] = findValues

  implicit val reportCauseEncoder: JsonEncoder[ReportCause] =
    JsonEncoder[String].contramap(_.entryName)
  implicit val reportCauseDecoder: JsonDecoder[ReportCause] =
    JsonDecoder[String].map {
      case "Targeted"        => Targeted
      case "Dox"             => Dox
      case "Threat"          => Threat
      case "Explicit"        => Explicit
      case "GraphicViolence" => GraphicViolence
      case "Terrorism"       => Terrorism
      case "Profanity"       => Profanity
      case "Impersonation"   => Impersonation
      case "Racism"          => Racism
    }

  def getSeverity(cause: ReportCause): String = cause match {
    case Explicit | GraphicViolence | Terrorism =>
      ReportSeverity.Critical.entryName
    case Targeted | Dox | Threat            => ReportSeverity.Severe.entryName
    case Profanity | Impersonation | Racism => ReportSeverity.Moderate.entryName
    case _                                  => ReportSeverity.Low.entryName
  }
}
