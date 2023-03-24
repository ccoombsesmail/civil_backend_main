// package civil.directives

// import java.util.Properties

// import civil.models.enums._
// import edu.stanford.nlp.ling.CoreAnnotations
// import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations
// import edu.stanford.nlp.pipeline.{Annotation, StanfordCoreNLP}
// import edu.stanford.nlp.sentiment.SentimentCoreAnnotations

// import scala.collection.convert.wrapAll._
// import civil.models.enums._

// object SentimentAnalyzer {

//   val props = new Properties()
//   props.setProperty("annotators", "tokenize, ssplit, parse, sentiment")
//   val pipeline: StanfordCoreNLP = new StanfordCoreNLP(props)

//   def mainSentiment(input: String): Sentiment = Option(input) match {
//     case Some(text) if !text.isEmpty => extractSentiment(text)
//     case _ => throw new IllegalArgumentException("input can't be null or empty")
//   }

//   // Should weight by the length of the sentence.
//   private def extractSentiment(text: String): Sentiment = {
//     val textSentimentData = extractSentiments(text)
//     val avgSentimentScore = textSentimentData
//       .map(_._2).sum / textSentimentData.length.toFloat
//       // .maxBy { case (sentence, _) => sentence.length }
//     println(avgSentimentScore)
//     println(Sentiment.toSentiment(avgSentimentScore))
//     Sentiment.toSentiment(avgSentimentScore)
//   }

//   def extractSentiments(text: String): List[(String, Int)] = {
//     val annotation: Annotation = pipeline.process(text)
//     val sentences = annotation.get(classOf[CoreAnnotations.SentencesAnnotation])
//     sentences
//       .map(sentence => (sentence, sentence.get(classOf[SentimentCoreAnnotations.SentimentAnnotatedTree])))
//       .map { case (sentence, tree) => (sentence.toString, RNNCoreAnnotations.getPredictedClass(tree)) }
//       .toList
//   }

// }