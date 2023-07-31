package civil.services.comments

import civil.errors.AppError
import civil.errors.AppError.GeneralError
import civil.models.{CommentLiked, CommentLikes, UpdateCommentLikes}
import civil.models.NotifcationEvents.{CommentLike, GivingUserNotificationData}
import civil.models.actions.LikedState
import civil.repositories.comments.CommentLikesRepository
import civil.services.{
  AuthenticationService,
  AuthenticationServiceLive,
  KafkaProducerServiceLive
}
import io.scalaland.chimney.dsl.TransformerOps
import zio.{URLayer, ZIO, ZLayer}

import javax.sql.DataSource

trait CommentLikesService {
  def addRemoveCommentLikeOrDislike(
      jwt: String,
      jwtType: String,
      commentLikeDislikeData: UpdateCommentLikes
  ): ZIO[Any, AppError, CommentLiked]

  def addRemoveTribunalCommentLikeOrDislike(
      jwt: String,
      jwtType: String,
      commentLikeDislikeData: UpdateCommentLikes
  ): ZIO[Any, AppError, CommentLiked]
}

object CommentLikesService {
  def addRemoveCommentLikeOrDislike(
      jwt: String,
      jwtType: String,
      commentLikeDislikeData: UpdateCommentLikes
  ): ZIO[CommentLikesService, AppError, CommentLiked] =
    ZIO.serviceWithZIO[CommentLikesService](
      _.addRemoveCommentLikeOrDislike(jwt, jwtType, commentLikeDislikeData)
    )

  def addRemoveTribunalCommentLikeOrDislike(
      jwt: String,
      jwtType: String,
      commentLikeDislikeData: UpdateCommentLikes
  ): ZIO[CommentLikesService, AppError, CommentLiked] =
    ZIO.serviceWithZIO[CommentLikesService](
      _.addRemoveTribunalCommentLikeOrDislike(
        jwt,
        jwtType,
        commentLikeDislikeData
      )
    )
}

case class CommentLikesServiceLive(
    commentLikesRepo: CommentLikesRepository,
    authenticationService: AuthenticationService
) extends CommentLikesService {
  override def addRemoveCommentLikeOrDislike(
      jwt: String,
      jwtType: String,
      commentLikeDislikeData: UpdateCommentLikes
  ): ZIO[Any, AppError, CommentLiked] = {
    val kafka = new KafkaProducerServiceLive()

    for {
      userData <- authenticationService.extractUserData(jwt, jwtType)
      _ <- ZIO.logInfo("Updating Tribunal Comment Like")
      data <- commentLikesRepo.addRemoveCommentLikeOrDislike(
        commentLikeDislikeData
          .into[CommentLikes]
          .withFieldConst(_.userId, userData.userId)
          .withFieldConst(_.commentId, commentLikeDislikeData.id)
          .withFieldConst(_.likeState, commentLikeDislikeData.likeAction)
          .transform,
        commentLikeDislikeData.createdByUserId
      )
      (likeData, comment) = data
      _ <- ZIO
        .when(likeData.likeState == LikedState)(
          kafka.publish(
            CommentLike(
              eventType = "CommentLike",
              commentId = likeData.commentId,
              receivingUserId = commentLikeDislikeData.createdByUserId,
              givingUserData = GivingUserNotificationData(
                givingUserId = userData.userId,
                givingUserUsername = userData.username,
                givingUserTag = Some(userData.userCivilTag),
                givingUserIconSrc = Some(userData.userIconSrc)
              ),
              spaceId = comment.spaceId,
              discussionId = comment.discussionId
            ),
            commentLikeDislikeData.createdByUserId,
            CommentLike.commentLikeSerde
          )
        )
        .mapError(e => {
          GeneralError(e.toString)
        })
        .forkDaemon
    } yield likeData
  }

  override def addRemoveTribunalCommentLikeOrDislike(
      jwt: String,
      jwtType: String,
      commentLikeDislikeData: UpdateCommentLikes
  ): ZIO[Any, AppError, CommentLiked] = {
    for {
      userData <- authenticationService.extractUserData(jwt, jwtType)
      _ <- ZIO.logInfo("Updating Tribunal Comment Like")
      likeData <- commentLikesRepo.addRemoveTribunalCommentLikeOrDislike(
        commentLikeDislikeData
          .into[CommentLikes]
          .withFieldConst(_.commentId, commentLikeDislikeData.id)
          .withFieldConst(_.userId, userData.userId)
          .withFieldConst(_.likeState, commentLikeDislikeData.likeAction)
          .transform
      )
    } yield likeData

  }

}

object CommentLikesServiceLive {

  val layer: URLayer[
    CommentLikesRepository with AuthenticationService,
    CommentLikesService
  ] = ZLayer.fromFunction(CommentLikesServiceLive.apply _)

}
