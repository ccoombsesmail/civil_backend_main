package civil.directives

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter._
import akka.http.scaladsl._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model.{HttpEntity, HttpMethods, HttpRequest, HttpResponse, MediaRange, MediaTypes}
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import civil.models.{IncomingRecommendations, Score, UrlsForTFIDFConversion, Words}

import scala.concurrent.Future
import org.json4s._
import org.json4s.jackson.JsonMethods
import org.json4s.jackson.Serialization
import civil.config.Config
import civil.errors.AppError
import civil.errors.AppError.Unauthorized
import io.circe
import io.circe.generic.auto._

import java.util.UUID
import sttp.client3.httpclient.zio.HttpClientZioBackend
import sttp.client3._
import sttp.client3.circe._
import zio.ZIO
import zio.json.{DecoderOps, DeriveJsonCodec, JsonCodec}

case class MetaData(html: Option[String], author_name: String)

object OutgoingHttp {
  val AcceptJson = Accept(
    MediaRange(MediaTypes.`text/plain`),
    MediaRange(MediaTypes.`application/json`)
  )
  implicit val formats: Formats = DefaultFormats
  implicit val actorSystem: ActorSystem[Nothing] =
    ActorSystem[Nothing](Behaviors.empty, "alpakka-samples")
  import actorSystem.executionContext

  def extractEntityData(response: HttpResponse): Source[ByteString, _] =
    response match {
      case HttpResponse(OK, _, entity, _) => entity.dataBytes
      case notOkResponse =>
        Source.failed(new RuntimeException(s"illegal response $notOkResponse"))
    }

  def getTweetInfo(url: String) = {
    val httpRequest = HttpRequest(uri =
      s"https://publish.twitter.com/oembed?url=$url&theme=dark&chrome=nofooter"
    )
      .withHeaders(AcceptJson)

    val future: Future[MetaData] =
      Source
        .single(httpRequest) // : HttpRequest
        .mapAsync(1)(
          Http()(actorSystem.toClassic).singleRequest(_)
        ) // : HttpResponse
        .flatMapConcat(extractEntityData) // : ByteString
        .runWith(Sink.fold(ByteString.empty)(_ ++ _))
        .map(_.utf8String) map { result =>
        JsonMethods.parse(result).extract[MetaData]
      }
    future
  }
  def sendHTTPToMLService(path: String, urls: UrlsForTFIDFConversion) = {
    val httpRequest = HttpRequest(
      uri = s"${Config().getString("civil.ml_service")}/internal/$path",
      method = HttpMethods.POST,
      entity = HttpEntity(
        ByteString(
          s"""{"targetUrl":"${urls.targetUrl}", "compareUrl":"${urls.compareUrl}"}"""
        )
      )
    )
      .withHeaders(AcceptJson)
    val future =
      Source
        .single(httpRequest) // : HttpRequest
        .mapAsync(1)(
          Http()(actorSystem.toClassic).singleRequest(_)
        ) // : HttpResponse
        .flatMapConcat(extractEntityData) // : ByteString
        .runWith(Sink.fold(ByteString.empty)(_ ++ _))
        .map(_.utf8String) map { result =>
        JsonMethods.parse(result).extract[Score]
      }
    future
  }

  def getSimilarityScoresBatch(
      path: String,
      targetUrl: String,
      targetId: UUID,
      urlsById: Map[String, String]
  ) = {
    val httpRequest = HttpRequest(
      uri = s"${Config().getString("civil.ml_service")}/internal/$path",
      method = HttpMethods.POST,
      entity = HttpEntity(ByteString(s"""{"urlsById":${Serialization.write(
          urlsById
        )}, "targetUrl":"${targetUrl}", "targetId":"${targetId}"}"""))
    )
      .withHeaders(AcceptJson)
    val future =
      Source
        .single(httpRequest) // : HttpRequest
        .mapAsync(1)(
          Http()(actorSystem.toClassic).singleRequest(_)
        ) // : HttpResponse
        .flatMapConcat(extractEntityData) // : ByteString
        .runWith(Sink.fold(ByteString.empty)(_ ++ _))
        .map(_.utf8String) map { result =>
        {
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
        .single(httpRequest) // : HttpRequest
        .mapAsync(1)(
          Http()(actorSystem.toClassic).singleRequest(_)
        ) // : HttpResponse
        .flatMapConcat(extractEntityData) // : ByteString
        .runWith(Sink.fold(ByteString.empty)(_ ++ _))
        .map(_.utf8String) map { result =>
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

  object Permissions {
    implicit val codec: JsonCodec[Permissions] =
      DeriveJsonCodec.gen[Permissions]
  }

  case class AuthRes(
      pk: String,
      permissions: Permissions,
      name: Option[String],
      iconUrl: Option[String],
      headline: Option[String]
  )

  object AuthRes {
    implicit val codec: JsonCodec[AuthRes] = DeriveJsonCodec.gen[AuthRes]
  }

  def authenticateCivicTokenHeader(auth: String): ZIO[Any, AppError, Response[
    Either[ResponseException[String, circe.Error], AuthRes]
  ]] = {
//    for {
//      res <- Client.request(
//        s"${Config().getString("civil.misc_service")}/internal/civic-auth",
//        headers = Headers.apply("Authorization", s"Bearer $auth")
//      )
//    } yield res.body.asString(Charset.defaultCharset()).fromJson[AuthRes]
    val request = basicRequest
      .get(uri"${Config().getString("civil.misc_service")}/internal/civic-auth")
      .auth
      .bearer(auth)
      .header("accept", "application/json")
      .acceptEncoding("gzip, deflate, br")
      .response(asJson[AuthRes])

    val result = HttpClientZioBackend().flatMap { backend =>
      {
        val res = request.send(backend)
        res
      }
    }
    result.mapError(Unauthorized)

  }

}
