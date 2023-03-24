package civil.models.enums

import enumeratum._


sealed trait TopicCategories extends EnumEntry

case object TopicCategories extends Enum[TopicCategories] with CirceEnum[TopicCategories] with QuillEnum[TopicCategories] {

  case object Technology  extends TopicCategories
  case object Medicine extends TopicCategories
  case object Politics  extends TopicCategories
  case object General extends TopicCategories

  val values: IndexedSeq[TopicCategories] = findValues

}

