package civil.models.actions

import civil.models.CommentReply
import io.circe.{Decoder, Encoder}
import io.circe.generic.extras.Configuration
import io.circe.generic.semiauto.deriveEncoder
import io.getquill.MappedEncoding
import zio.json.{DeriveJsonCodec, DeriveJsonDecoder, DeriveJsonEncoder, JsonCodec, JsonDecoder, JsonEncoder, jsonDiscriminator}
import enumeratum._
sealed trait LikeAction
case object LikedState extends LikeAction {
  val likeValue = 1;
}
case object DislikedState extends LikeAction {
  val likeValue = -1;
}

case object NeutralState extends LikeAction {
  val likeValue = 0;

}

object LikeAction {
//  implicit val codec: JsonCodec[LikeAction] = DeriveJsonCodec.gen[LikeAction]
  implicit val likedStateEncoderCirce: Encoder[LikeAction] = Encoder[String].contramap {
    case DislikedState => "DislikedState"
    case NeutralState => "NeutralState"
    case LikedState => "LikedState"
  }

  implicit val likedStateDecoderCirce: Decoder[LikeAction] = Decoder[String].emap {
    case "DislikedState" => Right(DislikedState)
    case "NeutralState" => Right(NeutralState)
    case "LikedState" => Right(LikedState)
    case other => Left(s"Invalid LikedState: $other")
  }

  implicit val likedStateEncoder: JsonEncoder[LikeAction] = JsonEncoder[String].contramap {
    case DislikedState => "DislikedState"
    case NeutralState  => "NeutralState"
    case LikedState    => "LikedState"
  }

  implicit val likedStateDecoder: JsonDecoder[LikeAction] = JsonDecoder[String].map {
    case "DislikedState" => DislikedState
    case "NeutralState"  => NeutralState
    case "LikedState"    => LikedState
  }


  def withName(name: String): LikeAction = name match {
    case "LikedState" => LikedState
    case "DislikedState" => DislikedState
    case "NeutralState" => NeutralState
  }

  implicit val encodeLikeAction: MappedEncoding[LikeAction, String] =
    MappedEncoding[LikeAction, String](_.toString)

  implicit val decodeLikeAction: MappedEncoding[String, LikeAction] =
    MappedEncoding[String, LikeAction](str => LikeAction.withName(str))

}
