package civil.apis

import sttp.tapir.endpoint
import sttp.tapir.json.circe.jsonBody

object HealthCheck {
  val healthCheckEndpoint = endpoint.get.in("healthcheck").out(jsonBody[String])
}
