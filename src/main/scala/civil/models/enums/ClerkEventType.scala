package civil.models.enums

import enumeratum._


sealed abstract class ClerkEventType(override val entryName: String) extends EnumEntry

case object ClerkEventType extends Enum[ClerkEventType] with CirceEnum[ClerkEventType] {

  case object UserCreated extends ClerkEventType("user.created")
  case object UserUpdated extends ClerkEventType("user.updated")

  val values = findValues

}