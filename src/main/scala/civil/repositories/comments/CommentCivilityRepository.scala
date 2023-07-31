package civil.repositories.comments

import civil.errors.AppError
import civil.errors.AppError.InternalServerError
import civil.models.{CommentCivility, Comments, TribunalComments, Users}
import civil.models._
import civil.services.KafkaProducerServiceLive
import zio._

import java.util.UUID
import javax.sql.DataSource

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
  ): ZIO[
    CommentCivilityRepository,
    AppError,
    (CivilityGivenResponse, Comments)
  ] =
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

case class CommentCivilityRepositoryLive(dataSource: DataSource)
    extends CommentCivilityRepository {
  val kafka = new KafkaProducerServiceLive()
  import civil.repositories.QuillContext._

  private def addCivility(
      rootId: Option[UUID],
      givingUserId: String,
      givingUserUsername: String,
      civilityData: UpdateCommentCivility
  ) = {
    (for {
      preUpdateCommentCivility <-
        run(
          query[CommentCivility].filter(cv =>
            cv.userId == lift(givingUserId) && cv.commentId == lift(
              civilityData.commentId
            )
          )
        )
      preUpdateCommentCivilityData = preUpdateCommentCivility.headOption
      civilityGivenRes <- (transaction {
        (for {
          civilityGivenRes <- run(
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
          _ <- run(
            updateUserCivilityQuery(
              civilityData.receivingUserId,
              civilityData.value - preUpdateCommentCivilityData
                .getOrElse(
                  CommentCivility(
                    userId = givingUserId,
                    commentId = civilityData.commentId,
                    value = 0f
                  )
                )
                .value
            )
          )
        } yield civilityGivenRes).mapError(e => {
          e
        })
      }).mapError(e => {
        e
      })
    } yield civilityGivenRes)
      .mapError(e => InternalServerError(e.toString))
      .provideEnvironment(ZEnvironment(dataSource))
  }

  override def addOrRemoveCommentCivility(
      givingUserId: String,
      givingUserUsername: String,
      civilityData: UpdateCommentCivility
  ): ZIO[Any, AppError, (CivilityGivenResponse, Comments)] = {

    (for {
      comment <- run(
        query[Comments].filter(c => c.id == lift(civilityData.commentId))
      )
      commentData <- ZIO
        .fromOption(comment.headOption)

      civilityGiven <- addCivility(
        commentData.rootId,
        givingUserId,
        givingUserUsername,
        civilityData
      )
    } yield (civilityGiven, commentData))
      .mapError(e => InternalServerError(e.toString))
      .provideEnvironment(ZEnvironment(dataSource))

  }

  private def updateUserCivilityQuery(
      receivingUserId: String,
      civility: Float
  ) =
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
    (for {
      comment <- run(
        query[TribunalComments].filter(c =>
          c.id == lift(civilityData.commentId)
        )
      )
      commentData <- ZIO
        .fromOption(comment.headOption)

      civilityGiven <- addCivility(
        commentData.rootId,
        givingUserId,
        givingUserUsername,
        civilityData
      )
    } yield civilityGiven)
      .mapError(e => InternalServerError(e.toString))
      .provideEnvironment(ZEnvironment(dataSource))
  }

}

object CommentCivilityRepositoryLive {

  val layer: URLayer[DataSource, CommentCivilityRepository] =
    ZLayer.fromFunction(CommentCivilityRepositoryLive.apply _)

}
