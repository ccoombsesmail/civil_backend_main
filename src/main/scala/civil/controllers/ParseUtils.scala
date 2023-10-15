package civil.controllers

import civil.errors.AppError
import civil.errors.AppError.{JsonDecodingError, Unauthorized}
import civil.models.{CommentId, DiscussionId, SpaceId}
import zio.http._
import zio.{IO, Task, ZIO}
import zio.json.{DecoderOps, EncoderOps, JsonCodec, JsonDecoder}

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

  def parseBody[A: JsonDecoder](request: Request): IO[AppError, A] = {

    for {
      stringBody <- request.body
        .asString
        .mapError(AppError.JsonDecodingError)
      parsed <- ZIO
        .from(stringBody.fromJson[A])
        .mapError(d => AppError.JsonDecodingError(new Throwable(d)))
    } yield parsed
  }

  def extractJwtData(request: Request): ZIO[Any, AppError, (String, String)] =
    for {
      jwt <- ZIO
        .fromOption(request.headers.get("authorization"))
        .orElseFail(Unauthorized(new Throwable("No Auth Token Provided")))
      jwtTypeHeader <- ZIO
        .fromOption(request.headers.get("X-JWT-TYPE"))
        .orElseFail(JsonDecodingError(new Throwable("error")))
      jwtType = jwtTypeHeader
    } yield (jwt, jwtType)

  def parseQuery(
      req: Request,
      paramName: String
  ): IO[AppError, List[String]] = {
    req.url.queryParams.get(paramName) match {
      case None =>
        ZIO.fail(
          JsonDecodingError(
            new Throwable(s"Failed to parse query parameter '$paramName'")
          )
        )
      case Some(value) => ZIO.succeed(value.toList)
    }
  }

  def parseQueryFirst(req: Request, key: String): IO[AppError, String] =
    parseQuery(req, key).flatMap {
      case head :: _ => ZIO.succeed(head)
      case Nil =>
        ZIO.fail(JsonDecodingError(new Throwable(s"Missing parameter: $key")))
    }

  def parseSpaceId(id: String): IO[AppError.InvalidIdError, SpaceId] =
    SpaceId
      .fromString(id)
      .orElseFail(AppError.InvalidIdError("Invalid space id"))
  def parseDiscussionId(id: String): IO[AppError.InvalidIdError, DiscussionId] =
    DiscussionId
      .fromString(id)
      .orElseFail(AppError.InvalidIdError("Invalid discussion id"))

  def parseCommentId(id: String): IO[AppError.InvalidIdError, CommentId] =
    CommentId
      .fromString(id)
      .orElseFail(AppError.InvalidIdError("Invalid comment id"))
  def parseSkip(value: String): IO[AppError.InvalidIdError, Skip] =
    Skip
      .fromString(value)
      .orElseFail(AppError.InvalidIdError("Invalid Skip Parameter"))

//  def parseFollowedUserId(value: String): IO[AppError.InvalidIdError, FollowedUserId] =
//    FollowedUserId.fromString(value).orElseFail(AppError.InvalidIdError("Invalid Followed User Id Parameter"))

}
