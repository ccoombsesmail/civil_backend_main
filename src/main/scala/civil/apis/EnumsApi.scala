package civil.apis
import io.circe.{Decoder, Encoder}
import sttp.tapir.json.circe._
import sttp.tapir.generic.auto._
import io.circe.generic.auto._
import civil.apis.BaseApi.baseEndpoint
import civil.models.UpdateLikes
import civil.models.enums.TopicCategories
import sttp.tapir._

object EnumsApi {

  val getAllEnumsEndpoint: Endpoint[Unit, Unit, IndexedSeq[TopicCategories], Any] =
    endpoint.in("api" / "v1").get
      .in("enums")
      .out(jsonBody[IndexedSeq[TopicCategories]])
}
