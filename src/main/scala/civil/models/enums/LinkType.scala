package civil.models.enums

import civil.models.ExternalContentData
import enumeratum._
import zio.json.{DeriveJsonCodec, JsonCodec, JsonDecoder, JsonEncoder}


sealed trait LinkType extends EnumEntry

case object LinkType extends Enum[LinkType] with CirceEnum[LinkType] with QuillEnum[LinkType] {

  case object YouTube  extends LinkType
  case object Twitter extends LinkType
  case object Web  extends LinkType

  case object Ipfs  extends LinkType


  val values: IndexedSeq[LinkType] = findValues

  implicit val linkTypeEncoder: JsonEncoder[LinkType] = JsonEncoder[String].contramap(_.entryName)
  implicit val linkTypeDecoder: JsonDecoder[LinkType] = JsonDecoder[String].map {
    case "YouTube" => YouTube
    case "Twitter" => Twitter
    case "Web" => Web
    case "Ipfs" => Ipfs

  }

}