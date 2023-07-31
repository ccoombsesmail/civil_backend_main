package civil.services

import civil.errors.AppError
import civil.models.{
  IncomingComment,
  TribunalCommentNode,
  TribunalComments,
  TribunalCommentsReply
}
import civil.models.enums.{Sentiment, TribunalCommentType}
import civil.repositories.TribunalCommentsRepository
import zio._
import civil.repositories.UsersRepository
import io.scalaland.chimney.dsl._

import java.time.{LocalDateTime, ZoneId, ZonedDateTime}
import java.util.UUID
// import civil.directives.SentimentAnalyzer

trait TribunalCommentsService {
  def insertComment(
      jwt: String,
      jwtType: String,
      incomingComment: IncomingComment
  ): ZIO[Any, AppError, TribunalCommentsReply]
  def getComments(
      jwt: String,
      jwtType: String,
      contentId: UUID,
      commentType: TribunalCommentType
  ): ZIO[Any, AppError, List[TribunalCommentNode]]
  def getCommentsBatch(
      jwt: String,
      jwtType: String,
      contentId: UUID
  ): ZIO[Any, AppError, List[TribunalCommentNode]]

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
  def getComments(
      jwt: String,
      jwtType: String,
      contentId: UUID,
      commentType: TribunalCommentType
  ): ZIO[TribunalCommentsService, AppError, List[TribunalCommentNode]] =
    ZIO.serviceWithZIO[TribunalCommentsService](
      _.getComments(jwt, jwtType, contentId, commentType)
    )
  def getCommentsBatch(
      jwt: String,
      jwtType: String,
      contentId: UUID
  ): ZIO[TribunalCommentsService, AppError, List[TribunalCommentNode]] =
    ZIO.serviceWithZIO[TribunalCommentsService](
      _.getCommentsBatch(jwt, jwtType, contentId)
    )

}

case class TribunalCommentsServiceLive(
    tribunalCommentsRepo: TribunalCommentsRepository,
    authenticationService: AuthenticationService
) extends TribunalCommentsService {

  override def insertComment(
      jwt: String,
      jwtType: String,
      incomingComment: IncomingComment
  ): ZIO[Any, AppError, TribunalCommentsReply] = {
    for {
      userData <- authenticationService.extractUserData(jwt, jwtType)
      insertedComment <- tribunalCommentsRepo.insertComment(
        incomingComment
          .into[TribunalComments]
          .withFieldConst(_.id, UUID.randomUUID())
          .withFieldConst(
            _.createdAt,
            ZonedDateTime.now(ZoneId.systemDefault())
          )
          .withFieldConst(_.likes, 0)
          .withFieldConst(_.sentiment, Sentiment.POSITIVE.toString)
          .withFieldConst(_.reportedContentId, incomingComment.contentId)
          .withFieldConst(_.createdByUserId, userData.userId)
//          .withFieldConst(
//            _.createdByUserId,
//            incomingComment.createdByUserId.get
//          )
          .transform,
        userData
      )
    } yield insertedComment

  }

  override def getComments(
      jwt: String,
      jwtType: String,
      contentId: UUID,
      commentType: TribunalCommentType
  ): ZIO[Any, AppError, List[TribunalCommentNode]] = {

    for {
      userData <- authenticationService.extractUserData(jwt, jwtType)
      comments <- tribunalCommentsRepo.getComments(
        userData.userId,
        contentId,
        commentType
      )
    } yield comments
  }

  override def getCommentsBatch(
      jwt: String,
      jwtType: String,
      contentId: UUID
  ): ZIO[Any, AppError, List[TribunalCommentNode]] = {

    for {
      userData <- authenticationService.extractUserData(jwt, jwtType)
      comments <- tribunalCommentsRepo.getCommentsBatch(
        userData.userId,
        contentId
      )
    } yield comments
  }

}

object TribunalCommentsServiceLive {

  val layer: URLayer[
    TribunalCommentsRepository with UsersRepository with AuthenticationService,
    TribunalCommentsService
  ] = ZLayer.fromFunction(TribunalCommentsServiceLive.apply _)

}
