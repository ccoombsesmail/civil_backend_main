package civil.services.comments

import civil.models.enums.Sentiment
import civil.models._
import civil.repositories.comments.CommentsRepository
import civil.repositories.topics.DiscussionRepository
import civil.services.{
  AuthenticationService,
  AuthenticationServiceLive,
  HTMLSanitizerLive
}
import zio._
// import civil.models.enums.{Sentiment}
import civil.repositories.UsersRepository
import io.scalaland.chimney.dsl._

import java.time.LocalDateTime
import java.util.UUID
// import civil.directives.SentimentAnalyzer

trait CommentsService {
  def insertComment(
      jwt: String,
      jwtType: String,
      incomingComment: IncomingComment
  ): ZIO[Any, ErrorInfo, CommentReply]
  def getComments(
      jwt: String,
      jwtType: String,
      discussionId: UUID,
      skip: Int
     ): ZIO[Any, ErrorInfo, List[CommentNode]]
  def getComment(
      jwt: String,
      jwtType: String,
      commentId: UUID
  ): ZIO[Any, ErrorInfo, CommentReply]
  def getAllCommentReplies(
      jwt: String,
      jwtType: String,
      commentId: UUID
  ): ZIO[Any, ErrorInfo, CommentWithReplies]

  def getUserComments(
      jwt: String,
      jwtType: String,
      userId: String
  ): ZIO[Any, ErrorInfo, List[CommentNode]]
}

object CommentsService {
  def insertComment(
      jwt: String,
      jwtType: String,
      incomingComment: IncomingComment
  ): ZIO[Has[CommentsService], ErrorInfo, CommentReply] =
    ZIO.serviceWith[CommentsService](
      _.insertComment(jwt, jwtType, incomingComment)
    )
  def getComments(
      jwt: String,
      jwtType: String,
      discussionId: UUID,
      skip: Int
  ): ZIO[Has[CommentsService], ErrorInfo, List[CommentNode]] =
    ZIO.serviceWith[CommentsService](_.getComments(jwt, jwtType, discussionId, skip))
  def getComment(
      jwt: String,
      jwtType: String,
      commentId: UUID
  ): ZIO[Has[CommentsService], ErrorInfo, CommentReply] =
    ZIO.serviceWith[CommentsService](_.getComment(jwt, jwtType, commentId))
  def getAllCommentReplies(
      jwt: String,
      jwtType: String,
      commentId: UUID
  ): ZIO[Has[CommentsService], ErrorInfo, CommentWithReplies] =
    ZIO.serviceWith[CommentsService](
      _.getAllCommentReplies(jwt, jwtType, commentId)
    )

  def getUserComments(
      jwt: String,
      jwtType: String,
      userId: String
  ): ZIO[Has[CommentsService], ErrorInfo, List[CommentNode]] =
    ZIO.serviceWith[CommentsService](_.getUserComments(jwt, jwtType, userId))

}

case class CommentsServiceLive(
    commentsRepo: CommentsRepository,
    usersRepo: UsersRepository,
    discussionRepo: DiscussionRepository,
    authenticationService: AuthenticationService
) extends CommentsService {

  override def insertComment(
      jwt: String,
      jwtType: String,
      incomingComment: IncomingComment
  ): ZIO[Any, ErrorInfo, CommentReply] = {
    // val sentiment = SentimentAnalyzer.mainSentiment(incommingComment.rawText)

    for {
      userData <- authenticationService.extractUserData(jwt, jwtType)
      insertedComment <- commentsRepo.insertComment(
        incomingComment
          .into[Comments]
          .withFieldConst(_.id, UUID.randomUUID())
          .withFieldConst(_.createdAt, LocalDateTime.now())
          .withFieldConst(_.likes, 0)
          .withFieldConst(_.sentiment, Sentiment.POSITIVE.toString)
          .withFieldConst(_.discussionId, incomingComment.contentId)
          .withFieldConst(_.createdByUserId, userData.userId)
          .transform,
        userData
      )
    } yield insertedComment

  }

  override def getComments(
      jwt: String,
      jwtType: String,
      discussionId: UUID,
      skip: Int
  ): ZIO[Any, ErrorInfo, List[CommentNode]] = {

    for {
      userData <- authenticationService.extractUserData(jwt, jwtType)
      comments <- commentsRepo.getComments(userData.userId, discussionId, skip)
    } yield comments
  }

  override def getComment(
      jwt: String,
      jwtType: String,
      commentId: UUID
  ): ZIO[Any, ErrorInfo, CommentReply] = {
    for {
      userData <- authenticationService.extractUserData(jwt, jwtType)
      comments <- commentsRepo.getComment(userData.userId, commentId)
    } yield comments
  }

  override def getAllCommentReplies(
      jwt: String,
      jwtType: String,
      commentId: UUID
  ): ZIO[Any, ErrorInfo, CommentWithReplies] = {
    for {
      userData <- authenticationService.extractUserData(jwt, jwtType)
      commentWithReplies <- commentsRepo.getAllCommentReplies(
        userData.userId,
        commentId
      )
    } yield commentWithReplies

  }

  override def getUserComments(
      jwt: String,
      jwtType: String,
      userId: String
  ): ZIO[Any, ErrorInfo, List[CommentNode]] = {
    for {
      userData <- authenticationService.extractUserData(jwt, jwtType)
      comments <- commentsRepo.getUserComments(
        userData.userId,
        userId
      )
    } yield comments
  }

}

object CommentsServiceLive {
  val live: ZLayer[Has[CommentsRepository]
    with Has[
      UsersRepository
    ]
    with Has[DiscussionRepository]
    with Has[AuthenticationService], Throwable, Has[CommentsService]] = {
    for {
      commentsRepo <- ZIO.service[CommentsRepository]
      usersRepo <- ZIO.service[UsersRepository]
      discussionRepo <- ZIO.service[DiscussionRepository]
      authenticationService <- ZIO.service[AuthenticationService]
    } yield CommentsServiceLive(
      commentsRepo,
      usersRepo,
      discussionRepo,
      authenticationService
    )
  }.toLayer
}
