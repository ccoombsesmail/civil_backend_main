package civil.models.enums

import enumeratum.{CirceEnum, Enum, EnumEntry, QuillEnum}
import zio.json.{DeriveJsonCodec, JsonCodec}


sealed trait UserVerificationType extends EnumEntry


case object UserVerificationType extends Enum[UserVerificationType] with CirceEnum[UserVerificationType] with QuillEnum[UserVerificationType] {

  case object CAPTCHA_VERIFIED  extends UserVerificationType
  case object FACE_ID_VERIFIED extends UserVerificationType
  case object FACE_ID_AND_CAPTCHA_VERIFIED  extends UserVerificationType
  case object NO_VERIFICATION  extends UserVerificationType

  val values: IndexedSeq[UserVerificationType] = findValues

  implicit val codec: JsonCodec[UserVerificationType] = DeriveJsonCodec.gen[UserVerificationType]


}

