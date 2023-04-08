package civil.models.enums

import civil.models.ExternalContentData
import enumeratum._
import zio.json.{DeriveJsonCodec, JsonCodec}


sealed trait LinkType extends EnumEntry

case object LinkType extends Enum[LinkType] with CirceEnum[LinkType] with QuillEnum[LinkType] {

  case object YouTube  extends LinkType
  case object Twitter extends LinkType
  case object Web  extends LinkType

  val values: IndexedSeq[LinkType] = findValues

  implicit val codec: JsonCodec[LinkType] = DeriveJsonCodec.gen[LinkType]

}