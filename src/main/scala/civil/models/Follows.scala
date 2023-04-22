package civil.models

import zio.json.{DeriveJsonCodec, JsonCodec}

import java.util.UUID

case class Follows(
    userId: String,
    followedUserId: String
)

case class FollowedUserId(
    followedUserId: String
)


object FollowedUserId {
  implicit val codec: JsonCodec[FollowedUserId] = DeriveJsonCodec.gen[FollowedUserId]

}