package civil.models.enums

import enumeratum._
import zio.json.{DeriveJsonCodec, JsonCodec, JsonDecoder, JsonEncoder}


sealed trait TopicCategories extends EnumEntry

case object TopicCategories extends Enum[TopicCategories] with CirceEnum[TopicCategories] with QuillEnum[TopicCategories] {

  case object Technology  extends TopicCategories
  case object Medicine extends TopicCategories
  case object Politics  extends TopicCategories
  case object General extends TopicCategories


  val values: IndexedSeq[TopicCategories] = findValues

  val list: List[String] = List(Technology.entryName, Medicine.entryName, Politics.entryName, General.entryName)

  implicit val linkTypeEncoder: JsonEncoder[TopicCategories] = JsonEncoder[String].contramap(_.entryName)
  implicit val linkTypeDecoder: JsonDecoder[TopicCategories] = JsonDecoder[String].map {
    case "Technology" => Technology
    case "Medicine" => Medicine
    case "Politics" => Politics
    case "General" => General

  }


}


