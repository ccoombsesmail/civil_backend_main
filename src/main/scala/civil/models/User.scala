package civil.models

import civil.directives.OutgoingHttp.Permissions
import civil.models.ClerkModels.ClerkUserPatch
import civil.models.enums.ClerkEventType

import java.util.UUID
import sttp.tapir.generic.auto._
import sttp.tapir.Schema
import zio.json.{DeriveJsonCodec, DeriveJsonDecoder, DeriveJsonEncoder, JsonCodec, JsonDecoder, JsonEncoder}

import java.time.LocalDateTime
import scala.math
import scala.math.{exp, round}

case class Users(
    userId: String,
    username: String,
    tag: Option[String],
    iconSrc: Option[String],
    civility: Float,
    createdAt: LocalDateTime,
    consortiumMember: Boolean = false,
    bio: Option[String],
    experience: Option[String],
    isDidUser: Boolean,
    id: Int = 200

                )

case class OutgoingUser(
    userId: String,
    username: String,
    tag: Option[String],
    iconSrc: Option[String],
    civility: Float,
    createdAt: LocalDateTime,
    consortiumMember: Boolean = false,
    isFollowing: Option[Boolean],
    bio: Option[String],
    experience: Option[String],
    isDidUser: Boolean,
    numFollowers: Option[Int] = None,
    numFollowed: Option[Int] = None,
    numPosts: Option[Int] = None,
    userLevelData: Option[UserLevel],
    permissions: Permissions = Permissions(false, false),
    id: Int = 200

)

object OutgoingUser {
  implicit val codec: JsonCodec[OutgoingUser] = DeriveJsonCodec.gen[OutgoingUser]
}

case class IncomingUser(
    userId: String,
    username: String,
    iconSrc: Option[String]
)

object IncomingUser {
  implicit val codec: JsonCodec[IncomingUser] = DeriveJsonCodec.gen[IncomingUser]
}

case class UpdateUserIcon(username: String, iconSrc: String) {}
object UpdateUserIcon {
  implicit val codec: JsonCodec[UpdateUserIcon] = DeriveJsonCodec.gen[UpdateUserIcon]
}
case class UpdateUserBio(bio: Option[String], experience: Option[String])
object UpdateUserBio {
  implicit val codec: JsonCodec[UpdateUserBio] = DeriveJsonCodec.gen[UpdateUserBio]
}
case class TagData(tag: String)

object TagData {
  implicit val codec: JsonCodec[TagData] = DeriveJsonCodec.gen[TagData]
}

case class TagExists(tagExists: Boolean)

object TagExists {
  implicit val codec: JsonCodec[TagExists] = DeriveJsonCodec.gen[TagExists]
}

case class WebHookEvent(
    data: WebHookData,
    `object`: String,
    `type`: ClerkEventType
)

case class WebHookData(
    birthday: String,
    created_at: Long,
    email_addresses: Seq[EmailData],
    external_accounts: Seq[ExternalAccountsData],
    external_id: Option[String],
    first_name: Option[String],
    gender: String,
    id: String,
    last_name: Option[String],
    `object`: String,
    password_enabled: Boolean,
    phone_numbers: Seq[String],
    primary_email_address_id: String,
    primary_phone_number_id: Option[String],
    primary_web3_wallet_id: Option[String],
    private_metadata: PrivateMetadata,
    profile_image_url: String,
    public_metadata: PublicMetadata,
    two_factor_enabled: Boolean,
    unsafe_metadata: UnsafeMetadata,
    updated_at: Long,
    username: Option[String],
    web3_wallets: Seq[Web3Wallet]
)

case class EmailData(
    email_address: String,
    id: String,
    linked_to: Seq[LinkedToData],
    `object`: String,
    verification: VerificationData
)

case class LinkedToData(
    id: Option[String],
    `type`: Option[String]
)

case class VerificationData(
    attempts: Option[Int],
    expire_at: Option[Long],
    status: Option[String],
    strategy: Option[String]
)

case class ExternalAccountsData(
    approved_scopes: Option[String],
    email_address: Option[String],
    family_name: Option[String],
    given_name: Option[String],
    google_id: Option[String],
    id: Option[String],
    `object`: Option[String],
    picture: Option[String]
)

case class Web3Wallet(
    id: String,
    `object`: String,
    verification: Option[Web3WalletVerification],
    web3_wallet: String
)

case class Web3WalletVerification(
    attempts: Int,
    expire_at: Long,
    nonce: String,
    status: String,
    strategy: String
)

case class PrivateMetadata()

object PrivateMetadata {
  implicit val decoder: JsonDecoder[PrivateMetadata] =
    DeriveJsonDecoder.gen[PrivateMetadata]
  implicit val encoder: JsonEncoder[PrivateMetadata] =
    DeriveJsonEncoder.gen[PrivateMetadata]
}
case class PublicMetadata(
    consortiumMember: Option[Boolean] = Some(false),
    userCivilTag: Option[String] = None
)

object PublicMetadata {
  implicit val decoder: JsonDecoder[PublicMetadata] =
    DeriveJsonDecoder.gen[PublicMetadata]
  implicit val encoder: JsonEncoder[PublicMetadata] =
    DeriveJsonEncoder.gen[PublicMetadata]
}

case class UnsafeMetadata(
    userCivilTag: Option[String] = None
)

object UnsafeMetadata {
  implicit val decoder: JsonDecoder[UnsafeMetadata] =
    DeriveJsonDecoder.gen[UnsafeMetadata]
  implicit val encoder: JsonEncoder[UnsafeMetadata] =
    DeriveJsonEncoder.gen[UnsafeMetadata]
}

case class JwtUserClaimsData(
    userId: String,
    username: String,
    userCivilTag: String,
    userIconSrc: String,
    civicHeadline: Option[String] = None,
    permissions: Permissions = Permissions(false, false),
    experience: Option[String]
)


case class UserLevel(exp: Double, level: Int, pointsForNextLevel: Double)

object UserLevel {
  val LEVELS: Double = 40.0
  val xp_for_first_level: Double = 5.0
  val xp_for_last_level: Double = 1000000.0
  val B: Double = math.log(xp_for_last_level / xp_for_first_level) / (LEVELS - 1)
  val A: Double = xp_for_first_level / (scala.math.exp(B) - 1.0)
  def apply(exp: Double): UserLevel = {
    val currLevel = (scala.math.log(exp/A) / B).toInt
    val currLevelExp = calcExpBasedOnLevel(currLevel)
    val expForNextLevel = calcExpBasedOnLevel(currLevel + 1) - currLevelExp
    new UserLevel(exp - currLevelExp, currLevel, expForNextLevel)
  }

  def calcExpBasedOnLevel(level: Int): Double = {
    val expPoints: Double = A * exp(B * (level));
    expPoints
  }

}