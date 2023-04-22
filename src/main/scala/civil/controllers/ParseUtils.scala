package civil.controllers


import civil.errors.AppError
import civil.errors.AppError.JsonDecodingError
import civil.models.{CommentId, DiscussionId, FollowedUserId, TopicId}
import zio.http._
import zio.http.model.HTTP_CHARSET
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
      stringBody <- request.body.asString(HTTP_CHARSET).mapError(e => AppError.JsonDecodingError(e.toString))
      _ = println(stringBody)
      parsed <- ZIO.from(stringBody.fromJson[A]).mapError(e => AppError.JsonDecodingError(e))
    } yield parsed
  }

  def extractJwtData(request: Request): IO[AppError, (String, String)] =
    for {
      jwt <- ZIO.fromOption(request.bearerToken).mapError(e => JsonDecodingError(e.toString))
      jwtTypeHeader <- ZIO.fromOption(request.header("X-JWT-TYPE")).mapError(e => JsonDecodingError(e.toString))
      jwtType = jwtTypeHeader.value.toString
    } yield (jwt, jwtType)


  def parseQuery(req: Request, paramName: String): IO[AppError, List[String]] = {
    req.url.queryParams.get(paramName) match {
      case None =>  ZIO.fail(JsonDecodingError(s"Failed to parse query parameter '$paramName'"))
      case Some(value) => ZIO.succeed(value.toList)
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

//  def parseFollowedUserId(value: String): IO[AppError.InvalidIdError, FollowedUserId] =
//    FollowedUserId.fromString(value).orElseFail(AppError.InvalidIdError("Invalid Followed User Id Parameter"))

}
