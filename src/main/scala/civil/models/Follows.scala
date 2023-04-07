package civil.models

import zio.{Random, Task, UIO, ZIO}
import zio.json.JsonCodec

import java.util.UUID

case class Follows(
    userId: String,
    followedUserId: String
)

case class FollowedUserId(
    value: String
)


object FollowedUserId {

  def fromString(id: String): Task[FollowedUserId] =
    ZIO.attempt {
      FollowedUserId(id)
    }

  implicit val codec: JsonCodec[FollowedUserId] =
    JsonCodec[String].transform(FollowedUserId(_), _.value)
}