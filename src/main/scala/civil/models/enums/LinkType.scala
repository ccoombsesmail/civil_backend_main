package civil.models.enums

import enumeratum._


sealed trait LinkType extends EnumEntry

case object LinkType extends Enum[LinkType] with CirceEnum[LinkType] with QuillEnum[LinkType] {

  case object YouTube  extends LinkType
  case object Twitter extends LinkType
  case object Web  extends LinkType

  val values: IndexedSeq[LinkType] = findValues

}