package civil.models

sealed trait ErrorInfo extends Product with Serializable

case class NotFound(userMsg: String) extends ErrorInfo
case class Unauthorized(userMsg: String) extends ErrorInfo
case class Unknown(code: Int, userMsg: String) extends ErrorInfo
case class InternalServerError(userMsg: String) extends ErrorInfo
case class BadRequest(userMsg: String) extends ErrorInfo
case object NoContent extends ErrorInfo
