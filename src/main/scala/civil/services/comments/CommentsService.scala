package civil.services.comments

import civil.errors.AppError
import civil.models.enums.Sentiment
import civil.models._
import civil.models.enums.ReportStatus.CLEAN
import civil.repositories.comments.CommentsRepository
import civil.repositories.discussions.DiscussionRepository
import civil.services.AuthenticationService
import zio._

import java.time.{ZoneId, ZonedDateTime}
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
                   ): ZIO[Any, AppError, CommentReply]

  def getComments(
                   jwt: String,
                   jwtType: String,
                   discussionId: UUID,
                   skip: Int
                 ): ZIO[Any, AppError, List[CommentNode]]

  def getCommentsUnauthenticated(
                                  discussionId: UUID,
                                  skip: Int
                                ): ZIO[Any, AppError, List[CommentNode]]

  def getComment(
                  jwt: String,
                  jwtType: String,
                  commentId: UUID
                ): ZIO[Any, AppError, CommentReplyWithParent]

  def getAllCommentReplies(
                            jwt: String,
                            jwtType: String,
                            commentId: UUID
                          ): ZIO[Any, AppError, CommentWithReplies]

  def getAllCommentRepliesUnauthenticated(
                                           commentId: UUID
                                         ): ZIO[Any, AppError, CommentWithReplies]

  def getUserComments(
                       jwt: String,
                       jwtType: String,
                       userId: String,
                       skip: Int
                     ): ZIO[Any, AppError, List[CommentNode]]

  def getUserCommentsUnauthenticated(
                                      userId: String,
                                      skip: Int
                                    ): ZIO[Any, AppError, List[CommentNode]]
}

object CommentsService {
  def insertComment(
                     jwt: String,
                     jwtType: String,
                     incomingComment: IncomingComment
                   ): ZIO[CommentsService, AppError, CommentReply] =
    ZIO.serviceWithZIO[CommentsService](
      _.insertComment(jwt, jwtType, incomingComment)
    )

  def getComments(
                   jwt: String,
                   jwtType: String,
                   discussionId: UUID,
                   skip: Int
                 ): ZIO[CommentsService, AppError, List[CommentNode]] =
    ZIO.serviceWithZIO[CommentsService](
      _.getComments(jwt, jwtType, discussionId, skip)
    )

  def getCommentsUnauthenticated(
                                  discussionId: UUID,
                                  skip: Int
                                ): ZIO[CommentsService, AppError, List[CommentNode]] =
    ZIO.serviceWithZIO[CommentsService](
      _.getCommentsUnauthenticated(discussionId, skip)
    )

  def getComment(
                  jwt: String,
                  jwtType: String,
                  commentId: UUID
                ): ZIO[CommentsService, AppError, CommentReplyWithParent] =
    ZIO.serviceWithZIO[CommentsService](_.getComment(jwt, jwtType, commentId))

  def getAllCommentReplies(
                            jwt: String,
                            jwtType: String,
                            commentId: UUID
                          ): ZIO[CommentsService, AppError, CommentWithReplies] =
    ZIO.serviceWithZIO[CommentsService](
      _.getAllCommentReplies(jwt, jwtType, commentId)
    )

  def getAllCommentRepliesUnauthenticated(
                                           commentId: UUID
                                         ): ZIO[CommentsService, AppError, CommentWithReplies] =
    ZIO.serviceWithZIO[CommentsService](
      _.getAllCommentRepliesUnauthenticated(commentId)
    )

  def getUserComments(
                       jwt: String,
                       jwtType: String,
                       userId: String,
                       skip: Int
                     ): ZIO[CommentsService, AppError, List[CommentNode]] =
    ZIO.serviceWithZIO[CommentsService](
      _.getUserComments(jwt, jwtType, userId, skip)
    )

  def getUserCommentsUnauthenticated(
                                      userId: String,
                                      skip: Int
                                    ): ZIO[CommentsService, AppError, List[CommentNode]] =
    ZIO.serviceWithZIO[CommentsService](
      _.getUserCommentsUnauthenticated(userId, skip)
    )
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
                            ): ZIO[Any, AppError, CommentReply] = {
    // val sentiment = SentimentAnalyzer.mainSentiment(incommingComment.rawText)

    for {
      userData <- authenticationService.extractUserData(jwt, jwtType)
      insertedComment <- commentsRepo.insertComment(
        incomingComment
          .into[Comments]
          .withFieldConst(_.id, UUID.randomUUID())
          .withFieldConst(
            _.createdAt,
            ZonedDateTime.now(ZoneId.systemDefault())
          )
          .withFieldConst(_.likes, 0)
          .withFieldConst(_.sentiment, Sentiment.POSITIVE.toString)
          .withFieldConst(_.discussionId, incomingComment.contentId)
          .withFieldConst(_.createdByUserId, userData.userId)
          .withFieldConst(_.reportStatus, CLEAN.toString)
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
                          ): ZIO[Any, AppError, List[CommentNode]] = {

    for {
      userData <- authenticationService.extractUserData(jwt, jwtType)
      comments <- commentsRepo.getComments(userData.userId, discussionId, skip)
    } yield comments
  }

  override def getCommentsUnauthenticated(
                                           discussionId: UUID,
                                           skip: RuntimeFlags
                                         ): ZIO[Any, AppError, List[CommentNode]] = {
    commentsRepo.getCommentsUnauthenticated(discussionId, skip)
  }

  override def getComment(
                           jwt: String,
                           jwtType: String,
                           commentId: UUID
                         ): ZIO[Any, AppError, CommentReplyWithParent] = {
    for {
      userData <- authenticationService.extractUserData(jwt, jwtType)
      comments <- commentsRepo.getComment(userData.userId, commentId)
    } yield comments
  }

  override def getAllCommentReplies(
                                     jwt: String,
                                     jwtType: String,
                                     commentId: UUID
                                   ): ZIO[Any, AppError, CommentWithReplies] = {
    for {
      userData <- authenticationService.extractUserData(jwt, jwtType)
      commentWithReplies <- commentsRepo.getAllCommentReplies(
        userData.userId,
        commentId
      )
    } yield commentWithReplies

  }

  override def getAllCommentRepliesUnauthenticated(
                                                    commentId: UUID
                                                  ): ZIO[Any, AppError, CommentWithReplies] = {
    commentsRepo.getAllCommentRepliesUnauthenticated(
      commentId
    )
  }

  override def getUserComments(
                                jwt: String,
                                jwtType: String,
                                userId: String,
                                skip: Int
                              ): ZIO[Any, AppError, List[CommentNode]] = {
    for {
      userData <- authenticationService.extractUserData(jwt, jwtType)
      comments <- commentsRepo.getUserComments(
        userData.userId,
        userId,
        skip
      )
    } yield comments
  }

  override def getUserCommentsUnauthenticated(userId: String, skip: Int): ZIO[Any, AppError, List[CommentNode]] = {
    commentsRepo.getUserCommentsUnauthenticated(
      userId,
      skip
    )

  }

}

object CommentsServiceLive {

  val layer: URLayer[
    CommentsRepository
      with UsersRepository
      with DiscussionRepository
      with AuthenticationService,
    CommentsService
  ] = ZLayer.fromFunction(CommentsServiceLive.apply _)

}
