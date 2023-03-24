package civil.models.ClerkModels

import civil.models.{PrivateMetadata, PublicMetadata, UnsafeMetadata}

import java.util.UUID

case class CreateClerkUser(
    external_id: String = UUID.randomUUID().toString,
    first_name: String = "Test User First Name",
    last_name: String = "Test User Last Name",
    email_address: Seq[String] = Seq("coombs.charles2@gmail.com"),
    phone_number: Seq[String] = Seq(),
    username: String = "Test User" + (Math.random() * 20000).toInt,
    password: String = "Testusertesuser1!",
    password_digest: String =
      "$2a$12$V6CrKTX284w.o//tbcGh7OSZbLAr74iI5btyz.uh2FSfrDbkpAHaO", // bcrypt hash of password
    password_hasher: String = "bcrypt",
    skip_password_checks: Boolean = true,
    skip_password_requirement: Boolean = true,
    public_metadata: PublicMetadata =
      PublicMetadata(consortiumMember = Some(false)),
    private_metadata: PrivateMetadata = PrivateMetadata(),
    unsafe_metadata: UnsafeMetadata = UnsafeMetadata()
) {

  def toMap: Map[String, String] = Map(
    "external_id" -> external_id,
    "first_name" -> first_name,
    "last_name" -> last_name,
    "email_address" -> email_address.toString(),
    "phone_number" -> phone_number.toString(),
    "username" -> username,
    "password" -> password,
    "password_digest" -> password_digest,
    "password_hasher" -> password_hasher,
    "skip_password_checks" -> skip_password_checks.toString,
    "skip_password_requirement" -> skip_password_requirement.toString,
    "public_metadata" -> public_metadata.toString,
    "private_metadata" -> private_metadata.toString,
    "unsafe_metadata" -> unsafe_metadata.toString
  )
}
