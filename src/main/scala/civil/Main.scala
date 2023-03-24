//package civil
//
//import zio.Schedule.Decision
//import zio._
//import zio.duration._
//import zio.console._
//import zio.clock._
//
//import java.util.Random
//import scala.language.postfixOps
//import scala.runtime.Nothing$
//
//
//object Main extends zio.App {
//
//  def makeRequest: Task[Int] = Task.effect {
//    new Random().nextInt(10)
//  }
//
//  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
//    val tappedSchedule = Schedule.spaced(1.second) && Schedule.recurUntil[Int](c => {
//      println(c)
//      c == 3
//    })
////    val tappedSchedule = Schedule.count.whileOutput(_ < 5).tapOutput(o => putStrLn(s"retrying $o").orDie)
////    implicit val rt: Runtime[Clock with Console] = Runtime.default
////
////    rt.unsafeRun(makeRequest.repeat(tappedSchedule).foldM(
////      ex => putStrLn("Exception Failed"),
////      v => putStrLn(s"Succeeded with $v"))
////    )
//    makeRequest.repeat(tappedSchedule).foldM(
//      ex => putStrLn("Exception Failed"),
//      v => putStrLn(s"Succeeded with $v")).exitCode
//  }
//
//
//
////    ZIO.succeed(1).schedule(tappedSchedule).exitCode
//    // Server.start(8092, app)
//    //   .provideCustomLayer(fullLayer)
//    //   .exitCode
//
//
//}
