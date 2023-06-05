package civil.models.enums

import enumeratum._
import zio.json.{JsonDecoder, JsonEncoder}

sealed trait SpaceCategories extends EnumEntry

case object SpaceCategories extends Enum[SpaceCategories] with CirceEnum[SpaceCategories] with QuillEnum[SpaceCategories] {

  case object Technology extends SpaceCategories
  case object Medicine extends SpaceCategories
  case object Politics extends SpaceCategories
  case object General extends SpaceCategories
  case object Science extends SpaceCategories
  case object Environment extends SpaceCategories
  case object Education extends SpaceCategories
  case object ArtsCulture extends SpaceCategories
  case object Entertainment extends SpaceCategories
  case object Sports extends SpaceCategories
  case object Travel extends SpaceCategories
  case object BusinessFinance extends SpaceCategories
  case object HealthWellness extends SpaceCategories
  case object FoodCooking extends SpaceCategories
  case object FashionBeauty extends SpaceCategories
  case object ParentingFamily extends SpaceCategories
  case object HomeGarden extends SpaceCategories
  case object DIYCrafts extends SpaceCategories
  case object PersonalDevelopment extends SpaceCategories
  case object CareerWork extends SpaceCategories
  case object Gaming extends SpaceCategories
  case object PetsAnimals extends SpaceCategories
  case object PhilosophyReligion extends SpaceCategories
  case object History extends SpaceCategories
  case object Literature extends SpaceCategories
  case object Music extends SpaceCategories
  case object FilmTelevision extends SpaceCategories
  case object Photography extends SpaceCategories
  case object SocialIssues extends SpaceCategories
  case object CurrentEvents extends SpaceCategories
  case object HobbiesInterests extends SpaceCategories
  case object FitnessExercise extends SpaceCategories
  case object Automotive extends SpaceCategories
  case object OutdoorAdventure extends SpaceCategories
  case object LanguageCommunication extends SpaceCategories
  case object RelationshipsDating extends SpaceCategories
  case object SpiritualityMindfulness extends SpaceCategories
  case object AstronomySpace extends SpaceCategories
  case object LawLegal extends SpaceCategories
  case object Psychology extends SpaceCategories

  val values: IndexedSeq[SpaceCategories] = findValues

  val list: List[String] = values.map(_.entryName).toList

  implicit val linkTypeEncoder: JsonEncoder[SpaceCategories] = JsonEncoder[String].contramap(_.entryName)
  implicit val linkTypeDecoder: JsonDecoder[SpaceCategories] = JsonDecoder[String].map(entryName => withNameInsensitive(entryName))

}