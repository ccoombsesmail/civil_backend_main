package civil.services

import civil.config.Config
import civil.directives.OutgoingHttp.authenticateCivicTokenHeader
import civil.errors.AppError
import civil.errors.AppError.{GeneralError, Unauthorized}
import civil.models.JwtUserClaimsData
import civil.repositories.UsersRepository
import org.elastos.did.{DID, DIDBackend, DefaultDIDAdapter}
import pdi.jwt.{JwtAlgorithm, JwtCirce, JwtClaim}
import org.json4s.jackson.JsonMethods

import scala.util.{Failure, Success}
import org.json4s.{DefaultFormats, Formats}
import zio._

import javax.sql.DataSource
import scala.language.postfixOps

trait AuthenticationService {
  def decodeClerkJWT(jwt: String): ZIO[Any, Throwable, JwtUserClaimsData]
  def decodeDIDJWT(
      jwt: String,
      did: String
  ): ZIO[Any, Throwable, JwtUserClaimsData]
  def civicAuthentication(jwt: String): ZIO[Any, Throwable, JwtUserClaimsData]

  def canPerformCaptchaRequiredAction(userData: JwtUserClaimsData): ZIO[Any, Unauthorized, Unit]

  def extractUserData(jwt: String,
                      jwtType: String): ZIO[Any, Unauthorized, JwtUserClaimsData]

}

object AuthenticationService {

  def decodeDIDJWT(jwt: String, did: String): RIO[AuthenticationService, JwtUserClaimsData] =
    ZIO.serviceWithZIO[AuthenticationService](_.decodeDIDJWT(jwt, did))

  def extractUserData(jwt: String,
                      jwtType: String):  ZIO[AuthenticationService, Unauthorized, JwtUserClaimsData]  =
    ZIO.serviceWithZIO[AuthenticationService](_.extractUserData(jwt,  jwtType))
}

case class AuthenticationServiceLive(dataSource: DataSource)
    extends AuthenticationService {
  implicit val formats: Formats = DefaultFormats

  val clerk_jwt_key = Config().getString("civil.clerk_jwt_key")


  override def extractUserData(
      jwt: String,
      jwtType: String
  ): ZIO[Any, Unauthorized, JwtUserClaimsData] = {
    for {
      jwtClaimsData <-
        jwtType match {
          case s"ELASTOS-DID ${didString}" =>
            decodeDIDJWT(jwt, didString).mapError(e =>
              Unauthorized("Failed Elatos DID Authentication")
            )
          case s"CIVIC-DID" =>
            civicAuthentication(jwt).mapError(e =>
              Unauthorized(s"Failed Civic/Solana DID Authentication Due To: ${e.toString}")
            )
          case "CLERK" =>
            decodeClerkJWT(jwt).mapError(e =>
              Unauthorized("Failed Clerk Authentication")
            )
          case _ => ZIO.fromOption(None).orElseFail(Unauthorized("Failed Auth"))
        }
    } yield jwtClaimsData
  }

  override def canPerformCaptchaRequiredAction(userData: JwtUserClaimsData): ZIO[Any, Unauthorized, Unit] = {
    ZIO.when(!userData.permissions.captchaPassActive)(
      ZIO.fail(
        Unauthorized(
          "Must Have An Active CAPTCHA pass to give civility points"
        )
      ))
    ZIO.unit
  }
  override def civicAuthentication(
      jwt: String
  ): ZIO[Any, AppError, JwtUserClaimsData] = {
    implicit val ec: scala.concurrent.ExecutionContext =
      scala.concurrent.ExecutionContext.global

    val decodedJwt = jwt match {
      case s"Bearer $authString" => authString
      case _                     => jwt
    }

    for {
      res <- authenticateCivicTokenHeader(decodedJwt).mapError(e => GeneralError(e.toString))
      body <- ZIO.fromEither(res.body).mapError(e => GeneralError(e.toString))
      userDataOpt <- UsersRepository.getUserInternal(body.pk).provideEnvironment(ZEnvironment(dataSource)).mapError(e => GeneralError(e.toString))
      userData <- ZIO.fromOption(userDataOpt).mapError(e => GeneralError(e.toString))
    } yield JwtUserClaimsData(
      userId = body.pk,
      username = body.name.getOrElse(body.pk),
      userCivilTag = userData.tag.getOrElse(""),
      userIconSrc = body.iconUrl.getOrElse(
        userData.iconSrc.getOrElse("https://civil-dev.s3.us-west-1.amazonaws.com/profile_img_1.png")
      ),
      civicHeadline = body.headline,
      permissions = body.permissions,
      experience = userData.experience
    )
  }

  override def decodeClerkJWT(
      jwt: String
  ): ZIO[Any, Throwable, JwtUserClaimsData] = {
    val decodedJwt = ZIO.attempt(jwt match {
      case s"Bearer $encodedJwt" =>
        JwtCirce.decode(
          encodedJwt,
          clerk_jwt_key,
          Seq(JwtAlgorithm.RS256)
        ) match {
          case Success(value)     => value
          case Failure(exception) => JwtClaim()
        }
      case _ => JwtClaim()
    })

    for {
      decoded <- decodedJwt
      _ = println(decoded)
    } yield JsonMethods.parse(decoded.content).extract[JwtUserClaimsData]

  }

  override def decodeDIDJWT(
      jwt: String,
      didString: String
  ): ZIO[Any, Throwable, JwtUserClaimsData] = {
    DIDBackend.initialize(new DefaultDIDAdapter("testnet"))
    val did = new DID(didString)
    val signer = did.resolve()

    if (!signer.isValid()) println("BAD SIGNER")

    val parser = signer.jwtParserBuilder.build()
    val decodedJwt = jwt match {
      case s"Bearer $encodedJwt" => parser.parseClaimsJws(encodedJwt)
      case _                     => parser.parseClaimsJwt(jwt)
    }
    val claims = decodedJwt.getBody()

    for {
      userData <- ZIO
        .fromOption(
          if (claims.containsKey("userId") && claims.containsKey("username")) {
            Some(
              JwtUserClaimsData(
                claims.get("userId").toString,
                claims.get("username").toString,
                claims
                  .getOrDefault("userCivilTag", claims.get("username").toString)
                  .toString,
                "https://civil-dev.s3.us-west-1.amazonaws.com/profile_img_1.png",
                experience = None
              )
            )
          } else None
        )
        .orElseFail(new Throwable("Failed Authentication"))
    } yield (userData)

  }

}

object AuthenticationServiceLive {

  val layer: URLayer[DataSource, AuthenticationService] = ZLayer.fromFunction(AuthenticationServiceLive.apply _)

}
