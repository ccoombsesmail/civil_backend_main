package civil.models.enums

import enumeratum._

sealed trait Sentiment extends EnumEntry with Serializable with Product

case object Sentiment extends Enum[Sentiment] with CirceEnum[Sentiment] {

    case object POSITIVE extends Sentiment
    case object NEUTRAL extends Sentiment
    case object NEGATIVE  extends Sentiment
    case object MEME extends Sentiment

  val values = findValues

  def toSentiment(sentiment: Float): Sentiment = {
    sentiment match {
      case x if x >= 0 && x <= 1 => Sentiment.NEGATIVE
      case x if x > 1 && x <= 2 => Sentiment.NEUTRAL
      case x if x > 2 => Sentiment.POSITIVE
      case _ => Sentiment.MEME
    }
    
  }



}

