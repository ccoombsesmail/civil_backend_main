package civil.controllers

import cats.conversions.all.autoWidenFunctor
import cats.implicits.toFoldableOps
import civil.errors.AppError
import civil.errors.AppError.JsonDecodingError
import civil.models.{CommentId, DiscussionId, FollowedUserId, TopicId}
import zhttp.http.Request
import zio.{IO, Random, Task, UIO, ZIO}
import zio.json.{DecoderOps, JsonCodec, JsonDecoder}

import java.util.UUID


case class Skip(value: Int)

object Skip {
  def fromString(value: String): Task[Skip] =
    ZIO.attempt {
      Skip(value.toInt)
    }

  implicit val codec: JsonCodec[Skip] =
    JsonCodec[Int].transform(Skip(_), _.value)
}

object ParseUtils {

  def parseBody[A: JsonDecoder](request: Request): IO[AppError, A] =
    for {
      body <- request.bodyAsString.orElseFail(AppError.MissingBodyError)
      parsed <- ZIO.from(body.fromJson[A]).mapError(AppError.JsonDecodingError)
    } yield parsed


  def extractJwtData(request: Request): IO[AppError, Option[(String, String)]] =
    for {
      jwt <- ZIO.fromOption(request.bearerToken).mapError(e => JsonDecodingError(e.toString))
      jwtType <- ZIO.attempt(request.header("X-JWT-TYPE")).mapError(e => JsonDecodingError(e.toString))
    } yield {
      jwtType.map { case (_, jwtType) =>
        (jwt, jwtType.toString)
      }
    }

  def parseQuery[T](req: Request, paramName: String): IO[AppError, T] = {
    req.url.queryParams
      .collectFirst { case (k, v) if k == paramName => v }
      .map(e => e.headOption) match {
      case None =>  ZIO.fail(JsonDecodingError(s"Failed to parse query parameter '$paramName'"))
      case Some(value: T) => ZIO.succeed(value)
    }
  }

  def parseTopicId(id: String): IO[AppError.InvalidIdError, TopicId] =
    TopicId.fromString(id).orElseFail(AppError.InvalidIdError("Invalid topic id"))
  def parseDiscussionId(id: String): IO[AppError.InvalidIdError, DiscussionId] =
    DiscussionId.fromString(id).orElseFail(AppError.InvalidIdError("Invalid discussion id"))

  def parseCommentId(id: String): IO[AppError.InvalidIdError, CommentId] =
    CommentId.fromString(id).orElseFail(AppError.InvalidIdError("Invalid comment id"))
  def parseSkip(value: String): IO[AppError.InvalidIdError, Skip] =
    Skip.fromString(value).orElseFail(AppError.InvalidIdError("Invalid Skip Parameter"))

  def parseFollowedUserId(value: String): IO[AppError.InvalidIdError, FollowedUserId] =
    FollowedUserId.fromString(value).orElseFail(AppError.InvalidIdError("Invalid Followed User Id Parameter"))

}
