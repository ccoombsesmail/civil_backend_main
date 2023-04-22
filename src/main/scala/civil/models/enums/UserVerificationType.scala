package civil.models.enums

import enumeratum.{CirceEnum, Enum, EnumEntry, QuillEnum}
import zio.json.{DeriveJsonCodec, JsonCodec, JsonDecoder, JsonEncoder}


sealed trait UserVerificationType extends EnumEntry


case object UserVerificationType extends Enum[UserVerificationType] with CirceEnum[UserVerificationType] with QuillEnum[UserVerificationType] {

  case object CAPTCHA_VERIFIED  extends UserVerificationType
  case object FACE_ID_VERIFIED extends UserVerificationType
  case object FACE_ID_AND_CAPTCHA_VERIFIED  extends UserVerificationType
  case object NO_VERIFICATION  extends UserVerificationType

  val values: IndexedSeq[UserVerificationType] = findValues

  implicit val linkTypeEncoder: JsonEncoder[UserVerificationType] = JsonEncoder[String].contramap(_.entryName)
  implicit val linkTypeDecoder: JsonDecoder[UserVerificationType] = JsonDecoder[String].map {
    case "CAPTCHA_VERIFIED" => CAPTCHA_VERIFIED
    case "FACE_ID_VERIFIED" => FACE_ID_VERIFIED
    case "FACE_ID_AND_CAPTCHA_VERIFIED" => FACE_ID_AND_CAPTCHA_VERIFIED
    case "NO_VERIFICATION" => NO_VERIFICATION

  }

}

