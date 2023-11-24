package civil.models

import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder
import zio.json.{DeriveJsonCodec, JsonCodec}

case class CreatedByUserData(
    createdByUsername: String,
    createdByUserId: String,
    createdByIconSrc: String,
    createdByTag: Option[String],
    civilityPoints: Long,
    numFollowers: Option[Int],
    numFollowed: Option[Int],
    numPosts: Option[Long],
    createdByExperience: Option[String] = None
)


object CreatedByUserData {
  implicit val encoder: Encoder[CreatedByUserData] = deriveEncoder[CreatedByUserData]
  implicit val codec: JsonCodec[CreatedByUserData] =
    DeriveJsonCodec.gen[CreatedByUserData]
}