package civil.repositories.comments

import civil.errors.AppError
import civil.errors.AppError.InternalServerError

import civil.models.{CommentCivility, Comments, AppError, TribunalComments, Unknown, Users}
import civil.models.NotifcationEvents.CommentCivilityGiven
import civil.models._
import civil.repositories.{QuillContextHelper, QuillContextQueries}
import civil.repositories.QuillContextQueries.getCommentsWithReplies
import civil.services.KafkaProducerServiceLive
import zio._

import java.util.UUID

trait CommentCivilityRepository {
  def addOrRemoveCommentCivility(
      givingUserId: String,
      givingUserUsername: String,
      civilityData: UpdateCommentCivility
  ): ZIO[Any, AppError, (CivilityGivenResponse, Comments)]
  def addOrRemoveTribunalCommentCivility(
      givingUserId: String,
      givingUserUsername: String,
      civilityData: UpdateCommentCivility
  ): ZIO[Any, AppError, CivilityGivenResponse]

}

object CommentCivilityRepository {
  def addOrRemoveCommentCivility(
      givingUserId: String,
      givingUserUsername: String,
      civilityData: UpdateCommentCivility
  ): ZIO[CommentCivilityRepository, AppError, (CivilityGivenResponse, Comments)] =
    ZIO.serviceWithZIO[CommentCivilityRepository](
      _.addOrRemoveCommentCivility(
        givingUserId,
        givingUserUsername,
        civilityData
      )
    )
  def addOrRemoveTribunalCommentCivility(
      givingUserId: String,
      givingUserUsername: String,
      civilityData: UpdateCommentCivility
  ): ZIO[CommentCivilityRepository, AppError, CivilityGivenResponse] =
    ZIO.serviceWithZIO[CommentCivilityRepository](
      _.addOrRemoveTribunalCommentCivility(
        givingUserId,
        givingUserUsername,
        civilityData
      )
    )

}

case class CommentCivilityRepositoryLive() extends CommentCivilityRepository {
  val kafka = new KafkaProducerServiceLive()
  import QuillContextHelper.ctx._

  private def addCivility(
      rootId: Option[UUID],
      givingUserId: String,
      givingUserUsername: String,
      civilityData: UpdateCommentCivility
  ) =
    for {
      preUpdateCommentCivility <- ZIO.attempt(
        run(
          query[CommentCivility].filter(cv =>
            cv.userId == lift(givingUserId) && cv.commentId == lift(
              civilityData.commentId
            )
          )
        ).headOption
      ).mapError(e => InternalServerError(e.toString))
      user <- ZIO
        .attempt(
          transaction {
            run(
              query[CommentCivility]
                .insertValue(
                  lift(
                    CommentCivility(
                      givingUserId,
                      civilityData.commentId,
                      civilityData.value
                    )
                  )
                )
                .onConflictUpdate(_.commentId, _.userId)((t, e) =>
                  t.value -> e.value
                )
                .returning(c =>
                  CivilityGivenResponse(
                    c.value,
                    lift(civilityData.commentId),
                    lift(rootId)
                  )
                )
            )
            run(
              updateUserCivilityQuery(
                civilityData.receivingUserId,
                civilityData.value - preUpdateCommentCivility.getOrElse(CommentCivility(userId = givingUserId, commentId = civilityData.commentId, value = 0f)).value
              )
            )
          }
        )
        .mapError(e => InternalServerError(e.toString))
    } yield CivilityGivenResponse(
      civility = civilityData.value,
      commentId = civilityData.commentId,
      rootId = rootId
    )

  override def addOrRemoveCommentCivility(
      givingUserId: String,
      givingUserUsername: String,
      civilityData: UpdateCommentCivility
  ): ZIO[Any, AppError, (CivilityGivenResponse, Comments)] = {

    for {
      comment <- ZIO
        .fromOption(
          run(
            query[Comments].filter(c => c.id == lift(civilityData.commentId))
          ).headOption
        )
        .orElseFail(InternalServerError("Can't Find Comment"))
      civilityGiven <- addCivility(
        comment.rootId,
        givingUserId,
        givingUserUsername,
        civilityData
      ).mapError(e => InternalServerError(e.toString))
    } yield (civilityGiven, comment)

  }

  def updateUserCivilityQuery(receivingUserId: String, civility: Float) =
    quote {
      query[Users]
        .filter(u => u.userId == lift(receivingUserId))
        .update(user => user.civility -> (user.civility + lift(civility)))
        .returning(r => r)
    }

  override def addOrRemoveTribunalCommentCivility(
      givingUserId: String,
      givingUserUsername: String,
      civilityData: UpdateCommentCivility
  ): ZIO[Any, AppError, CivilityGivenResponse] = {
    for {
      comment <- ZIO
        .fromOption(
          run(
            query[TribunalComments].filter(c =>
              c.id == lift(civilityData.commentId)
            )
          ).headOption
        )
        .orElseFail(InternalServerError("Can't Find Comment"))
      civilityGiven <- addCivility(
        comment.rootId,
        givingUserId,
        givingUserUsername,
        civilityData
      ).mapError(e => InternalServerError(e.toString))

    } yield civilityGiven
  }

}

object CommentCivilityRepositoryLive {

  val layer: URLayer[Any, CommentCivilityRepository] = ZLayer.fromFunction(CommentCivilityRepositoryLive.apply _)

}
