package civil.models.NotifcationEvents

import zio.ZIO
import zio.json.{DecoderOps, DeriveJsonDecoder, DeriveJsonEncoder, EncoderOps, JsonDecoder, JsonEncoder}
import zio.kafka.serde.Serde

case class NewFollower(
    eventType: String,
    followedUserId: String,
    givingUserData: GivingUserNotificationData,
)


object NewFollower {
  implicit val decoder: JsonDecoder[NewFollower] = DeriveJsonDecoder.gen[NewFollower]
  implicit val encoder: JsonEncoder[NewFollower] = DeriveJsonEncoder.gen[NewFollower]

  val newFollowerSerde: Serde[Any, NewFollower] = Serde.string.inmapM { newFollowerAsString =>
    ZIO.fromEither(newFollowerAsString.fromJson[NewFollower].left.map(new RuntimeException(_)))
  } { newFollowerAsObj =>
    ZIO.attempt(newFollowerAsObj.toJson)
  }
}


