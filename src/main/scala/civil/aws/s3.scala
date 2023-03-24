// // package civil.aws

// // import akka.stream.alpakka.s3.scaladsl.S3
// // import akka.stream.scaladsl.{ Sink, Source }
// // import akka.util.ByteString
// // import akka.NotUsed
// // import scala.io.Codec
// // import scala.concurrent.Future
// // import akka.stream.alpakka.s3.MultipartUploadResult
// // import akka.stream.ActorMaterializer
// // import akka.actor.ActorSystem
// // import com.typesafe.config.ConfigFactory
// // import akka.stream.alpakka.s3.S3Headers
// // import akka.stream.alpakka.s3.MetaHeaders
// // import java.nio.file.Files;
// // import java.io.File;
// object s3 extends App {
//   case class Node(id: Int, name: String)

//     val l = Seq(Node(1, "hey"), Node(2, "lol"), Node(1, "sdfsf"))
//     println(l.groupBy(_.id))
// //   val conf = ConfigFactory.load();
// //   implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
// //   implicit val codec = Codec("ISO-8859-1")
// //   implicit val system = ActorSystem("s3")
// //   implicit val materializer = ActorMaterializer()
// //   val metaHeaders: Map[String, String] = Map("location" -> "us-west-1", "datatype" -> "image")

// //   val fi = new File("./64_1.png");
// //   val fileContent = Files.readAllBytes(fi.toPath())

// //   // val f = scala.io.Source.fromFile("./64_1.png").mkString

// //   val file: Source[ByteString, NotUsed] =
// //     Source.single(ByteString(fileContent))

// //   val s3Sink: Sink[ByteString, Future[MultipartUploadResult]] =
// //     S3.multipartUpload("civil-dev", "profile_img_1.png")
// //   // println(f.getBytes().length)
// //   // println(f.length)
// //   // val res = S3.putObject(
// //   //   "civil-dev",
// //   //   "profile_img_3.png",
// //   //   file,
// //   //   f.getBytes().length,
// //   //   s3Headers = S3Headers().withMetaHeaders(MetaHeaders(metaHeaders)))
// //   // .runWith(Sink.head)

// //   //  res.onComplete(r => println(r))

// //   val result: Future[MultipartUploadResult] =
// //     file.runWith(s3Sink)

// //   result.onComplete(r => println(r))
// //   // println(result)
// //   // def run(args: List[String]): URIO[ZEnv,ExitCode] = {
// //   //     val f = scala.io.Source.fromFile("./64_1.png").mkString
// //   //     val file: Source[ByteString, NotUsed] =
// //   //       Source.single(ByteString(f))

// //   //     ZIO.effectTotal(
// //   //       println(file)
// //   //     ).exitCode

// }
