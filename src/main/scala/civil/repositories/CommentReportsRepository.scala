//package civil.repositories
//
//import civil.models._
//import civil.models.enums.ReportStatus
//import zio.duration.{Duration, durationInt}
//import zio.{Fiber, Has, Schedule, ZIO, ZLayer, console}
//
//import java.io.IOException
//import java.time.Instant
//import java.util.UUID
//
//trait CommentReportsRepository {
//  def addCommentReport(commentReport: CommentReports): ZIO[Any, ErrorInfo, Unit]
//  def getCommentReport(
//      commentId: UUID,
//      userId: String
//  ): ZIO[Any, ErrorInfo, CommentReportInfo]
//
//}
//
//object CommentReportsRepository {
//  def addCommentReport(
//      CommentReport: CommentReports
//  ): ZIO[Has[CommentReportsRepository], ErrorInfo, Unit] =
//    ZIO.serviceWith[CommentReportsRepository](
//      _.addCommentReport(CommentReport)
//    )
//
//  def getCommentReport(
//      CommentId: UUID,
//      userId: String
//  ): ZIO[Has[CommentReportsRepository], ErrorInfo, CommentReportInfo] =
//    ZIO.serviceWith[CommentReportsRepository](
//      _.getCommentReport(CommentId, userId)
//    )
//}
//
//case class CommentReportsRepositoryLive() extends CommentReportsRepository {
//  val runtime = zio.Runtime.default
//  import QuillContextHelper.ctx._
//
//  val VOTE_THRESHOLD = 2
//
//  override def addCommentReport(
//      CommentReport: CommentReports
//  ): ZIO[Any, ErrorInfo, Unit] = {
//    val res = for {
//      _ <- ZIO
//        .effect(
//          run(
//            query[CommentReports].insert(lift(CommentReport))
//          )
//        )
//        .mapError(e => InternalServerError(e.toString))
//      allCommentReports <- ZIO
//        .effect(
//          run(
//            query[CommentReports].filter(tr =>
//              tr.commentId == lift(CommentReport.commentId)
//            )
//          )
//        )
//        .mapError(e => InternalServerError(e.toString))
//      _ = if (allCommentReports.length >= 1)
//        setupCommentReviewProcess(CommentReport.commentId)
//    } yield ()
//    res
//
//  }
//
//  def setupCommentReviewProcess(commentId: UUID) = {
//
//    transaction {
//      run(
//        query[Comments]
//          .filter(t => t.id == lift(commentId))
//          .update(t => t.reportStatus -> "UNDER_REVIEW")
//      )
//      run(
//        query[ReportTiming].insert(
//          lift(ReportTiming(CommentId, Instant.now().toEpochMilli + 86400000))
//        )
//      )
//    }
//
//    startVotingTimer(CommentId)
//  }
//
//  def determineVoteResult(CommentId: UUID) = {
//
//    for {
//      votes <- ZIO.effect(
//        run(
//          query[CommentTribunalVotes].filter(ttv =>
//            ttv.CommentId == lift(CommentId)
//          )
//        )
//      )
//      numFor = votes.count(ttv => ttv.voteFor.contains(true))
//      numAgainst = votes.count(ttv => ttv.voteAgainst.contains(true))
//      _ = println(numFor, numAgainst)
//      reportStatus =
//        if (numFor >= numAgainst) ReportStatus.Clean.entryName
//        else ReportStatus.Removed.entryName
//      _ <- ZIO.effect(
//        transaction {
//          run(
//            query[ReportTiming]
//              .filter(rt => rt.CommentId == lift(CommentId))
//              .delete
//          )
//          run(
//            query[Comments]
//              .filter(t => t.id == lift(CommentId))
//              .update(_.reportStatus -> lift(reportStatus))
//          )
//        }
//      )
//    } yield true
//
//  }
//
//  def startVotingTimer(
//      CommentId: UUID
//  ): Fiber.Runtime[IOException, Duration] = {
//    println(Console.BLUE + "STARTED TIMER!!!")
//    println(Console.RESET + "")
//
//    val endVoting = for {
//      numVotes <- ZIO.effect(
//        run(
//          query[CommentTribunalVotes].filter(ttv =>
//            ttv.CommentId == lift(CommentId)
//          )
//        ).length
//      )
//      r <- numVotes match {
//        case x if x < VOTE_THRESHOLD =>
//          ZIO.effect(
//            run(
//              query[ReportTiming]
//                .filter(rt => rt.CommentId == lift(CommentId))
//                .update(
//                  _.reportPeriodEnd -> lift(
//                    Instant.now().toEpochMilli + 86400000
//                  )
//                )
//                .returning(_ => false)
//            )
//          )
//        case _ => determineVoteResult(CommentId)
//      }
//      _ = if (!r) startVotingTimer(CommentId)
//    } yield r
//
//    runtime.unsafeRun(for {
//      f <- endVoting
//        .catchAll(e => console.putStrLn(s"job failed with $e"))
//        .schedule(Schedule.duration(1.minute))
//        .forkDaemon
//    } yield f)
//  }
//
//  override def getCommentReport(
//      CommentId: UUID,
//      userId: String
//  ): ZIO[Any, ErrorInfo, CommentReportInfo] = {
//    for {
//      voteOpt <- ZIO
//        .effect(
//          run(
//            query[CommentTribunalVotes].filter(ttv =>
//              ttv.userId == lift(userId)
//            )
//          ).headOption
//        )
//        .mapError(e => InternalServerError(e.toString))
//      vote = voteOpt.getOrElse(
//        CommentTribunalVotes(
//          userId = userId,
//          CommentId = CommentId,
//          voteAgainst = None,
//          voteFor = None
//        )
//      )
//      timing <- ZIO
//        .fromOption(
//          run(
//            query[ReportTiming].filter(rt => rt.CommentId == lift(CommentId))
//          ).headOption
//        )
//        .orElseFail(
//          NotFound("Trouble Finding Comment Report Data")
//        )
//      CommentReports <- ZIO
//        .effect(
//          run(
//            query[CommentReports].filter(tr => tr.CommentId == lift(CommentId))
//          )
//        )
//        .mapError(e => InternalServerError(e.toString))
//      reportsMap = CommentReports.foldLeft(Map[String, Int]()) { (m, t) =>
//        val m1 = if (t.toxic.isDefined) {
//          if (m.contains("toxic"))
//            m + ("toxic" -> (m.getOrElse("toxic", 0) + 1))
//          else m + ("toxic" -> 1)
//        } else m
//
//        val m2 = if (t.personalAttack.isDefined) {
//          if (m1.contains("personalAttack"))
//            m1 + ("personalAttack" -> (m1.getOrElse("personalAttack", 0) + 1))
//          else m1 + ("personalAttack" -> 1)
//        } else m1
//
//        val m3 = if (t.spam.isDefined) {
//          if (m2.contains("spam"))
//            m2 + ("spam" -> (m2.getOrElse("spam", 0) + 1))
//          else m2 + ("spam" -> 1)
//        } else m2
//        m3
//      }
//    } yield CommentReportInfo(
//      CommentId,
//      reportsMap.getOrElse("toxic", 0),
//      reportsMap.getOrElse("personalAttack", 0),
//      reportsMap.getOrElse("spam", 0),
//      vote.voteAgainst,
//      vote.voteFor,
//      timing.reportPeriodEnd
//    )
//  }
//}
//
//object CommentReportsRepositoryLive {
//  val live: ZLayer[Any, Nothing, Has[
//    CommentReportsRepository
//  ]] = ZLayer.succeed(CommentReportsRepositoryLive())
//}
