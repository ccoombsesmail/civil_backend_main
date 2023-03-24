package civil.directives
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import pdi.jwt.{JwtCirce, JwtAlgorithm, JwtClaim}
import scala.util.Success
import io.circe._, io.circe.parser._
import scala.util.Failure
import java.util._
import java.time.Instant
import scala.collection.immutable

import io.circe.parser.decode

object Auth {
  val key = "secretKey"

  def authenticated: Directive0 = {
    optionalHeaderValueByName("Authorization").flatMap {
      case Some(jwt) if isTokenExpired(jwt) =>
        complete(StatusCodes.Unauthorized -> "Token expired")
      case Some(jwt) =>
        pass
      case _ => complete(StatusCodes.Unauthorized)
    }
  }
  def isTokenExpired(jwt: String) : Boolean = {
    val bearerStripped = jwt.split(" ")(1)
    val decoded = JwtCirce.decode(bearerStripped, key, Seq(JwtAlgorithm.HS256))
    decoded match {
      case Success(value) => 
        value.expiration match {
          case Some(exp) =>
            if (Instant.now.getEpochSecond > exp) true else false
          case None => 
            false
        }
      case Failure(exception) => 
        true
    }
  }
}
