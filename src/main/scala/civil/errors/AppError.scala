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
  def error: Throwable

  def userMsg: String

  def errorCode: Int

  def internalMsg: String

  override def getMessage: String = userMsg
}

object AppError {

  final case class NotFoundError(error: Throwable) extends AppError {
    val userMsg = "Sorry, and error occurred while processing your request"
    val internalMsg = s"Entity Not Found Error -> Cause: ${error.getMessage}"
    val errorCode = 500
  }
  final case class JsonDecodingError(error: Throwable) extends AppError {
    val userMsg = "Sorry, an error occurred while processing your request"
    val internalMsg = s"Decoding Error -> Cause: ${error.getMessage}"
    val errorCode = 500
  }

  final case class DatabaseError(error: Throwable) extends AppError {
    val userMsg = "Sorry, an error occurred while processing your request"
    val internalMsg = s"Database Error -> Cause: ${error.getMessage}}"
    val errorCode = 500
  }

  final case class InternalServerError(error: Throwable) extends AppError {
    val userMsg = "Sorry, an error occurred while processing your request"
    val internalMsg =
      s"General Internal Server Error -> Cause: ${error.getMessage}"
    val errorCode = 500
  }

  final case class Unauthorized(error: Throwable) extends AppError {
    val userMsg = "Sorry, it seems you are not authorized to make that request"
    val internalMsg = s"Unauthorized -> Cause: ${error.getMessage}"
    val errorCode = 401
  }

  final case class InvalidIdError(
      errorS: String,
      error: Throwable = new Throwable()
  ) extends AppError {
    val userMsg = "Sorry, it seems you are not authorized to make that request"
    val internalMsg = s"Invalid request -> Cause ${errorS}"
    val errorCode = 404
  }
  //  def logAppError(error: AppError, service: String): UIO[Unit] = {
  //    ZIO.logError(error.internalMsg)
  //  }

  implicit class AppErrorOps(val error: AppError) extends AnyVal {
    def toResponse: UIO[Response] = {
      for {
        _ <- ZIO.logInfo(error.internalMsg)
      } yield Response(
        Status.fromInt(error.errorCode).getOrElse(Status.UnprocessableEntity),
        body = Body.fromString(error.userMsg)
      )

    }
  }
}
