package civil.directives

import akka.Done
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter._
import akka.http.javadsl.model.headers.HttpCredentials.createOAuth2BearerToken
import akka.http.scaladsl._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model.{FormData, HttpEntity, HttpMethods, HttpRequest, HttpResponse, MediaRange, MediaTypes}
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import civil.models.{IncomingRecommendations, PrivateMetadata, PublicMetadata, Score, UnsafeMetadata, UrlsForTFIDFConversion, Words}

import scala.concurrent.Future
import org.json4s._
import org.json4s.jackson.JsonMethods
import org.json4s.jackson.Serialization
import civil.config.Config
import civil.models.ClerkModels.{ClerkResponse, ClerkUserPatch, CreateClerkUser}
import io.circe.syntax.EncoderOps
import io.circe.generic.auto._
import io.scalaland.chimney.dsl.TransformerOps
import sttp.model.HeaderNames.ContentType

import java.util.UUID
import scala.util.{Failure, Success}
import sttp.client3.httpclient.zio.HttpClientZioBackend
import sttp.client3._
import sttp.client3.circe._

import zio.ZIO



case class MetaData(html: Option[String], author_name: String)

object OutgoingHttp {
  val AcceptJson = Accept(MediaRange(MediaTypes.`text/plain`), MediaRange(MediaTypes.`application/json`))
  implicit val formats: Formats = DefaultFormats
  implicit val actorSystem: ActorSystem[Nothing] = ActorSystem[Nothing](Behaviors.empty, "alpakka-samples")
  import actorSystem.executionContext
 

  def extractEntityData(response: HttpResponse): Source[ByteString, _] =
    response match {
      case HttpResponse(OK, _, entity, _) => entity.dataBytes
      case notOkResponse =>
        Source.failed(new RuntimeException(s"illegal response $notOkResponse"))
    }


    def getTweetInfo(url: String) = {
      val httpRequest = HttpRequest(uri = s"https://publish.twitter.com/oembed?url=$url&theme=dark&chrome=nofooter")
        .withHeaders(AcceptJson)

      val future: Future[MetaData] =
       Source
        .single(httpRequest) //: HttpRequest
        .mapAsync(1)(Http()(actorSystem.toClassic).singleRequest(_)) //: HttpResponse
        .flatMapConcat(extractEntityData) //: ByteString
        .runWith(Sink.fold(ByteString.empty)(_ ++ _)).map(_.utf8String) map { result =>
          JsonMethods.parse(result).extract[MetaData]
        }
      future
  }
    def sendHTTPToMLService(path: String, urls: UrlsForTFIDFConversion) = {
      val httpRequest = HttpRequest(
        uri = s"${Config().getString("civil.ml_service")}/internal/$path",
        method = HttpMethods.POST,
        entity = HttpEntity(ByteString(s"""{"targetUrl":"${urls.targetUrl}", "compareUrl":"${urls.compareUrl}"}"""))
        )
        .withHeaders(AcceptJson)
      val future =
        Source
          .single(httpRequest) //: HttpRequest
          .mapAsync(1)(Http()(actorSystem.toClassic).singleRequest(_)) //: HttpResponse
          .flatMapConcat(extractEntityData) //: ByteString
          .runWith(Sink.fold(ByteString.empty)(_ ++ _)).map(_.utf8String) map { result =>
          JsonMethods.parse(result).extract[Score]
        }
      future
    }

  def getSimilarityScoresBatch(path: String, targetUrl: String, targetId: UUID, urlsById: Map[String, String]) = {
    val httpRequest = HttpRequest(
      uri = s"${Config().getString("civil.ml_service")}/internal/$path",
      method = HttpMethods.POST,
      entity = HttpEntity(ByteString(s"""{"urlsById":${Serialization.write(urlsById)}, "targetUrl":"${targetUrl}", "targetId":"${targetId}"}"""))
    )
      .withHeaders(AcceptJson)
    val future =
      Source
        .single(httpRequest) //: HttpRequest
        .mapAsync(1)(Http()(actorSystem.toClassic).singleRequest(_)) //: HttpResponse
        .flatMapConcat(extractEntityData) //: ByteString
        .runWith(Sink.fold(ByteString.empty)(_ ++ _)).map(_.utf8String) map { result => {
          Serialization.read[IncomingRecommendations](result)
        }
      }
    future
  }

  def getTopicWordsFromMLService(path: String, url: String) = {
    val httpRequest = HttpRequest(
      uri = s"${Config().getString("civil.ml_service")}/internal/$path",
      method = HttpMethods.POST,
      entity = HttpEntity(ByteString(s"""{"targetUrl":"${url}"}"""))
    )
      .withHeaders(AcceptJson)
    val future =
      Source
        .single(httpRequest) //: HttpRequest
        .mapAsync(1)(Http()(actorSystem.toClassic).singleRequest(_)) //: HttpResponse
        .flatMapConcat(extractEntityData) //: ByteString
        .runWith(Sink.fold(ByteString.empty)(_ ++ _)).map(_.utf8String) map { result =>
        JsonMethods.parse(result).extract[Words]
      }
    future
  }

  case class PublicKey(
      value: String
 )
  case class GatewayToken(
  issuingGatekeeper: String,
  gatekeeperNetwork: String,
  owner: String,
  state: String,
  publicKey: String,
  programId: String,
  expiryTime: Option[Long]
  )
  case class Content(
      gatewayTokens: Seq[GatewayToken],
      exp: Long
 )

  case class Permissions(
        faceIdPassActive: Boolean,
        captchaPassActive: Boolean
                        )
  case class AuthRes(
      pk: String,
      permissions: Permissions,
      name: Option[String],
      iconUrl: Option[String],
      headline: Option[String]
)

  def authenticateCivicTokenHeader(auth: String) = {
    println(uri"${Config().getString("civil.misc_service")}/internal/civic-auth")
    val request = basicRequest
      .get(uri"${Config().getString("civil.misc_service")}/internal/civic-auth")
      .auth.bearer(auth)
      .header("accept","application/json").acceptEncoding("gzip, deflate, br")
      .response(asJson[AuthRes])

    val result = HttpClientZioBackend().flatMap { backend => {
      val res = request.send(backend)
      res.tapError(e => {
        println(e.toString)
        ZIO.succeed(e)
      })
      res.mapError(e => println(e.toString))
      res
    }}
    result

  }


  def createClerkUser(user: CreateClerkUser) = {
    val form = FormData(user.toMap)
    val httpRequest = HttpRequest(
      uri = s"https://api.clerk.dev/v1/users",
      method = HttpMethods.POST,
      entity = form.toEntity
    )
      .withHeaders(AcceptJson)
      .addCredentials(createOAuth2BearerToken("test_NVpuvoQPIVbP5ALMSCBxeIAnwMHV5L8gmt"))

    val future =
      Source
        .single(httpRequest) //: HttpRequest
        .mapAsync(1)(Http()(actorSystem.toClassic).singleRequest(_)) //: HttpResponse
        .flatMapConcat(extractEntityData) //: ByteString
        .runWith(Sink.fold(ByteString.empty)(_ ++ _)).map(_.utf8String) map { result =>
        result
      }
    future
  }

  def updateClerkUserMetaData(userId: String, userTag: Option[String]) = {


    val httpRequest = HttpRequest(
      uri = s"https://api.clerk.dev/v1/users/${userId}",
      method = HttpMethods.GET,
//      entity = form.toEntity
    )
      .withHeaders(AcceptJson)
      .addCredentials(createOAuth2BearerToken("test_NVpuvoQPIVbP5ALMSCBxeIAnwMHV5L8gmt"))
    val future =
      Source
        .single(httpRequest) //: HttpRequest
        .mapAsync(1)(Http()(actorSystem.toClassic).singleRequest(_)) //: HttpResponse
        .flatMapConcat(extractEntityData) //: ByteString
        .runWith(Sink.fold(ByteString.empty)(_ ++ _)).map(_.utf8String) map { result =>
        println(result)
        val res = JsonMethods.parse(result).extract[ClerkResponse]
        println(res)
        res
      }

    future onComplete {
      case Success(user) => {


        val j = user.into[ClerkUserPatch]
          .withFieldConst(_.public_metadata, PublicMetadata(userCivilTag = userTag))
          .withFieldConst(_.private_metadata, PrivateMetadata())
          .withFieldConst(_.unsafe_metadata, UnsafeMetadata())
          .transform.asJson
        val httpRequest = HttpRequest(
          uri = s"https://api.clerk.dev/v1/users/${userId}",
          method = HttpMethods.PATCH,
        )
          .withEntity(ByteString(j.toString()))
          .withHeaders(AcceptJson)
          .addCredentials(createOAuth2BearerToken("test_NVpuvoQPIVbP5ALMSCBxeIAnwMHV5L8gmt"))
        val futurePatch =
          Source
            .single(httpRequest) //: HttpRequest
            .mapAsync(1)(Http()(actorSystem.toClassic).singleRequest(_)) //: HttpResponse
            .flatMapConcat(extractEntityData) //: ByteString
            .runWith(Sink.fold(ByteString.empty)(_ ++ _)).map(_.utf8String) map { result =>
            result
          }

        futurePatch onComplete {
          case Success(d) => println(d)
          case Failure(exception) => println(exception.toString)
        }

      }
      case Failure(exception) => println(exception.toString)

    }

    future
  }
}
