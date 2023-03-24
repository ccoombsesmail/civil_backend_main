package civil.services

import civil.models.{ErrorInfo, IncomingComment, TribunalCommentNode, TribunalComments, TribunalCommentsReply}
import civil.models.enums.{Sentiment, TribunalCommentType}
import civil.models._
import civil.repositories.{TribunalCommentsRepository, TribunalJuryRepository}
import zio._
// import civil.models.enums.{Sentiment}
import civil.repositories.UsersRepository
import io.scalaland.chimney.dsl._

import java.time.LocalDateTime
import java.util.UUID
// import civil.directives.SentimentAnalyzer

trait TribunalCommentsService {
  def insertComment(
      jwt: String,
      jwtType: String,
      incomingComment: IncomingComment
  ): ZIO[Any, ErrorInfo, TribunalCommentsReply]
  def getComments(jwt: String, jwtType: String, contentId: UUID, commentType: TribunalCommentType): ZIO[Any, ErrorInfo, List[TribunalCommentNode]]
  def getCommentsBatch(jwt: String, jwtType: String, contentId: UUID): ZIO[Any, ErrorInfo, List[TribunalCommentNode]]

  //  def addOrRemoveCommentLike(id: UUID, userId: String, increment: Boolean): Task[Liked]
}

object TribunalCommentsService {
  def insertComment(
      jwt: String,
      jwtType: String,
      incomingComment: IncomingComment
  ): ZIO[Has[TribunalCommentsService], ErrorInfo, TribunalCommentsReply] =
    ZIO.serviceWith[TribunalCommentsService](
      _.insertComment(jwt, jwtType, incomingComment)
    )
  def getComments(jwt: String, jwtType: String, contentId: UUID, commentType: TribunalCommentType): ZIO[Has[TribunalCommentsService], ErrorInfo, List[TribunalCommentNode]] =
    ZIO.serviceWith[TribunalCommentsService](_.getComments(jwt, jwtType, contentId, commentType))
  def getCommentsBatch(jwt: String, jwtType: String, contentId: UUID): ZIO[Has[TribunalCommentsService], ErrorInfo, List[TribunalCommentNode]] =
    ZIO.serviceWith[TribunalCommentsService](_.getCommentsBatch(jwt, jwtType, contentId))
//  def addOrRemoveCommentLike(id: UUID, userId: String, increment: Boolean): RIO[Has[CommentsService], Liked] =
//    ZIO.serviceWith[CommentsService](_.addOrRemoveCommentLike(id, userId, increment))
}

case class TribunalCommentsServiceLive(
    tribunalCommentsRepo: TribunalCommentsRepository,
) extends TribunalCommentsService {

  override def insertComment(
      jwt: String,
      jwtType: String,
      incomingComment: IncomingComment
  ): ZIO[Any, ErrorInfo, TribunalCommentsReply] = {
    // val sentiment = SentimentAnalyzer.mainSentiment(incommingComment.rawText)

    val authenticationService = AuthenticationServiceLive()

    for {
      userData <- authenticationService.extractUserData(jwt, jwtType)
      insertedComment <- tribunalCommentsRepo.insertComment(
        incomingComment
          .into[TribunalComments]
          .withFieldConst(_.id, UUID.randomUUID())
          .withFieldConst(_.createdAt, LocalDateTime.now())
          .withFieldConst(_.likes, 0)
          .withFieldConst(_.sentiment, Sentiment.POSITIVE.toString)
          .withFieldConst(_.reportedContentId, incomingComment.contentId)
          .withFieldConst(_.createdByUserId, userData.userId)
          .transform
      )
    } yield insertedComment

  }


  override def getComments(jwt: String, jwtType: String, contentId: UUID, commentType: TribunalCommentType): ZIO[Any, ErrorInfo, List[TribunalCommentNode]] = {
    val authenticationService = AuthenticationServiceLive()

    for {
      userData <- authenticationService.extractUserData(jwt, jwtType)
      comments <- tribunalCommentsRepo.getComments(userData.userId, contentId, commentType)
    } yield comments
  }

  override def getCommentsBatch(jwt: String, jwtType: String, contentId: UUID): ZIO[Any, ErrorInfo, List[TribunalCommentNode]] = {
    val authenticationService = AuthenticationServiceLive()

    for {
      userData <- authenticationService.extractUserData(jwt, jwtType)
      comments <- tribunalCommentsRepo.getCommentsBatch(userData.userId, contentId)
    } yield comments
  }
//
//  override def addOrRemoveCommentLike(id: UUID, userId: String, increment: Boolean): Task[Liked] = {
//    val addEntryInLikeTable = increment match {
//      case true => Try(insertLike(id, userId))
//      case _ => Try(deleteLike(id, userId))
//    }
//
//    addEntryInLikeTable match {
//      case Success(value) => commentsRepo.addOrRemoveCommentLike(id, increment)
//      case Failure(e) => {
//        ZIO.fail(e)
//      }
//    }
//  }

}

object TribunalCommentsServiceLive {
  val live: ZLayer[Has[TribunalCommentsRepository] with Has[
    UsersRepository
  ], Throwable, Has[TribunalCommentsService]] = {
    for {
      tribunalCommentsRepo <- ZIO.service[TribunalCommentsRepository]
    } yield TribunalCommentsServiceLive(tribunalCommentsRepo)
  }.toLayer
}
