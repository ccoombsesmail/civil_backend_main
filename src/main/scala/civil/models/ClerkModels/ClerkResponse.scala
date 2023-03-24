package civil.models.ClerkModels

import civil.models.{PrivateMetadata, PublicMetadata}

case class EmailAddresses(
    id: String,
    `object`: String,
    email_address: String,
    verification: EmailPhoneVerification,
    linked_to: Seq[LinkedTo]
)

case class PhoneNumbers(
    id: String,
    `object`: String,
    phone_number: String,
    reserved_for_second_factor: Boolean,
    verification: EmailPhoneVerification,
    linked_to: Seq[LinkedTo]
)

case class LinkedTo (
  `type`: String,
   id: String
)

case class ClerkResponse(
    id: String,
    `object`: String,
    external_id: String,
    username: String,
    first_name: String,
    last_name: String,
    profile_image_url: String,
    primary_email_address_id: String,
    primary_phone_number_id: String,
    primary_web3_wallet_id: String,
    password_enabled: Boolean,
    two_factor_enabled: Boolean,
    totp_enabled: Boolean,
    backup_code_enabled: Boolean,
    email_addresses: Seq[EmailAddresses],
    phone_numbers: Seq[PhoneNumbers],
    web3_wallets: Seq[Web3Wallets],
    external_accounts: Seq[ExternalAccounts],
    public_metadata: PublicMetadata,
    private_metadata: PrivateMetadata,
    created_at: Int,
    updated_at: Int
)

case class EmailPhoneVerification(
    status: String,
    strategy: String,
    attempts: Option[Int],
    expire_at: Option[Int]
)

case class Verification(
    status: String,
    strategy: String,
    attempts: Option[Int],
    expire_at: Int,
    nonce: Option[String]
)

case class Web3Wallets(
    id: String,
    `object`: String,
    web3_wallet: String,
    verification: Verification
)

case class ExternalAccounts (
  `object`: String,
  id: String,
  google_id: String,
  approved_scopes: String,
  email_address: String,
  given_name: String,
  family_name: String,
  picture: String,
  username: String,
  public_metadata: Any,
  label: String,
  verification: Verification
  )
