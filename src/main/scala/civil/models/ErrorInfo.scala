package civil.models

sealed trait AppError extends Product with Serializable

case class NotFound(userMsg: String) extends AppError
case class Unauthorized(userMsg: String) extends AppError
case class Unknown(code: Int, userMsg: String) extends AppError
case class BadRequest(userMsg: String) extends AppError
case object NoContent extends AppError
