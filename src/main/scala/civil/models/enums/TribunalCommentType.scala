package civil.models.enums

import enumeratum._


sealed trait TribunalCommentType extends EnumEntry

case object TribunalCommentType extends Enum[TribunalCommentType] with CirceEnum[TribunalCommentType] with QuillEnum[TribunalCommentType] {

  case object Defendant  extends TribunalCommentType
  case object Reporter extends TribunalCommentType
  case object Jury  extends TribunalCommentType
  case object General  extends TribunalCommentType

  val values: IndexedSeq[TribunalCommentType] = findValues

}

