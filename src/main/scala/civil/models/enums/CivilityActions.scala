package civil.models.enums

import enumeratum._

sealed trait CivilityActions extends EnumEntry

case object CivilityActions extends Enum[CivilityActions] with CirceEnum[CivilityActions] {

  case object AddC  extends CivilityActions
  case object Medicine extends CivilityActions
  case object Politics  extends CivilityActions
  case object General extends CivilityActions

  val values = findValues

}