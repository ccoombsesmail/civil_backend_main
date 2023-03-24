package civil.models.ClerkModels

import civil.models.{PrivateMetadata, PublicMetadata, UnsafeMetadata}
import io.circe.syntax.EncoderOps
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}
import io.circe.generic.auto._

case class ClerkUserPatch (
      external_id: Option[String],
      username: String,
      password: String = "dummy",
      first_name: String,
      last_name: String,
      primary_email_address_id: String,
      primary_phone_number_id: Option[String],
      public_metadata: PublicMetadata,
      private_metadata: PrivateMetadata,
      unsafe_metadata: UnsafeMetadata
  ) {


  def toMap: Map[String, String] = Map(
    "external_id" -> external_id.getOrElse("xxx"),
    "first_name" -> first_name,
    "last_name" -> last_name,
    "password" -> password,
    "primary_email_address_id" -> primary_email_address_id,
    "primary_phone_number_id" -> primary_phone_number_id.getOrElse("xxx-xxx-xxxx"),
    "username" -> username,
    "public_metadata" -> public_metadata.asJson.toString,
    "private_metadata" -> private_metadata.asJson.toString,
    "unsafe_metadata" -> unsafe_metadata.asJson.toString
  )
}

object ClerkUserPatch {
  implicit val decoder: JsonDecoder[ClerkUserPatch] =
    DeriveJsonDecoder.gen[ClerkUserPatch]
  implicit val encoder: JsonEncoder[ClerkUserPatch] =
    DeriveJsonEncoder.gen[ClerkUserPatch]
}
