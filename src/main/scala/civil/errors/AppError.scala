package civil.errors

import civil.models.IncomingPollVote
import zio.http.model.Status
import zio.{UIO, ZIO}
import zio.http.{Body, Response}
import zio.json.{DeriveJsonCodec, JsonCodec}

/** Here we have defined our own error type, called AppError, which is a subtype
  * of Throwable. The purpose of this is to make errors more descriptive and
  * easier to understand and therefore easier handle.
  */
sealed trait AppError extends Throwable {
  def userMsg: String
  def errorCode: Int
  override def getMessage: String = userMsg
}

object AppError {
  final case class JsonDecodingError(userMsg: String, internalMsg: String = "", errorCode: Int = 400) extends AppError
  final case class InvalidIdError(userMsg: String, internalMsg: String = "", errorCode: Int = 400) extends AppError
  final case class InternalServerError(userMsg: String, internalMsg: String = "", errorCode: Int = 400) extends AppError
  final case class GeneralError(userMsg: String, internalMsg: String = "", errorCode: Int = 400) extends AppError
  final case class Unauthorized(userMsg: String, internalMsg: String = "", errorCode: Int = 400) extends AppError

  implicit class AppErrorOps(val error: AppError) extends AnyVal {
    def toResponse: UIO[Response] = {
      ZIO.succeed(Response(Status.fromInt(error.errorCode).getOrElse(Status.UnprocessableEntity), body = Body.fromString(error.userMsg)))
    }
  }
}