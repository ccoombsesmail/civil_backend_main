package civil.services

import civil.errors.AppError
import civil.models.{IncomingComment, TribunalCommentNode, TribunalComments, TribunalCommentsReply}
import civil.models.enums.{Sentiment, TribunalCommentType}
import civil.repositories.TribunalCommentsRepository
import zio._
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
  ): ZIO[Any, AppError, TribunalCommentsReply]
  def getComments(jwt: String, jwtType: String, contentId: UUID, commentType: TribunalCommentType): ZIO[Any, AppError, List[TribunalCommentNode]]
  def getCommentsBatch(jwt: String, jwtType: String, contentId: UUID): ZIO[Any, AppError, List[TribunalCommentNode]]

  //  def addOrRemoveCommentLike(id: UUID, userId: String, increment: Boolean): Task[Liked]
}

object TribunalCommentsService {
  def insertComment(
      jwt: String,
      jwtType: String,
      incomingComment: IncomingComment
  ): ZIO[TribunalCommentsService, AppError, TribunalCommentsReply] =
    ZIO.serviceWithZIO[TribunalCommentsService](
      _.insertComment(jwt, jwtType, incomingComment)
    )
  def getComments(jwt: String, jwtType: String, contentId: UUID, commentType: TribunalCommentType): ZIO[TribunalCommentsService, AppError, List[TribunalCommentNode]] =
    ZIO.serviceWithZIO[TribunalCommentsService](_.getComments(jwt, jwtType, contentId, commentType))
  def getCommentsBatch(jwt: String, jwtType: String, contentId: UUID): ZIO[TribunalCommentsService, AppError, List[TribunalCommentNode]] =
    ZIO.serviceWithZIO[TribunalCommentsService](_.getCommentsBatch(jwt, jwtType, contentId))
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
  ): ZIO[Any, AppError, TribunalCommentsReply] = {
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


  override def getComments(jwt: String, jwtType: String, contentId: UUID, commentType: TribunalCommentType): ZIO[Any, AppError, List[TribunalCommentNode]] = {
    val authenticationService = AuthenticationServiceLive()

    for {
      userData <- authenticationService.extractUserData(jwt, jwtType)
      comments <- tribunalCommentsRepo.getComments(userData.userId, contentId, commentType)
    } yield comments
  }

  override def getCommentsBatch(jwt: String, jwtType: String, contentId: UUID): ZIO[Any, AppError, List[TribunalCommentNode]] = {
    val authenticationService = AuthenticationServiceLive()

    for {
      userData <- authenticationService.extractUserData(jwt, jwtType)
      comments <- tribunalCommentsRepo.getCommentsBatch(userData.userId, contentId)
    } yield comments
  }

}

object TribunalCommentsServiceLive {

  val layer: URLayer[TribunalCommentsRepository with UsersRepository, TribunalCommentsService] = ZLayer.fromFunction(TribunalCommentsServiceLive.apply _)

}
