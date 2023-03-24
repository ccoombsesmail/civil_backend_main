package civil.models.NotifcationEvents

import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

case class GivingUserNotificationData(
    givingUserUsername: String,
    givingUserId: String,
    givingUserTag: Option[String],
    givingUserIconSrc: Option[String]
)

object GivingUserNotificationData {
  implicit val decoder: JsonDecoder[GivingUserNotificationData] =
    DeriveJsonDecoder.gen[GivingUserNotificationData]
  implicit val encoder: JsonEncoder[GivingUserNotificationData] =
    DeriveJsonEncoder.gen[GivingUserNotificationData]
}