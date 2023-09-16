package civil.repositories

import civil.errors.AppError
import civil.errors.AppError.{DatabaseError, InternalServerError}
import civil.models._
import io.scalaland.chimney.dsl.TransformerOps
import zio._

import java.sql.SQLException
import java.time.{LocalDateTime, ZoneId, ZonedDateTime}
import java.util.UUID
import javax.sql.DataSource

trait UsersRepository {
  def upsertDidUser(
      incomingUser: IncomingUser
  ): ZIO[Any, AppError, OutgoingUser]

  def getUser(
      id: String,
      requesterId: String
  ): ZIO[Any, AppError, OutgoingUser]

  def getUserUnauthenticated(
      id: String
  ): ZIO[Any, AppError, OutgoingUserUnauthenticated]

  def updateUserIcon(
      username: String,
      iconSrc: String
  ): ZIO[Any, AppError, OutgoingUser]

  def updateUserBio(
      userId: String,
      bioInfo: UpdateUserBio
  ): ZIO[Any, AppError, OutgoingUser]

  def addOrRemoveCivility(
      userId: String,
      commentId: UUID,
      civility: Int,
      removeCivility: Boolean
  ): ZIO[Any, AppError, CivilityGivenResponse]

  def createUserTag(
      userId: String,
      tag: String
  ): ZIO[Any, AppError, OutgoingUser]

  def checkIfTagExists(tag: String): ZIO[Any, AppError, TagExists]

}

object UsersRepository {
  def upsertDidUser(
      incomingUser: IncomingUser
  ): ZIO[UsersRepository, AppError, OutgoingUser] =
    ZIO.serviceWithZIO[UsersRepository](_.upsertDidUser(incomingUser))

  def getUser(
      id: String,
      requesterId: String
  ): ZIO[UsersRepository, AppError, OutgoingUser] =
    ZIO.serviceWithZIO[UsersRepository](_.getUser(id, requesterId))

  def getUserUnauthenticated(
      id: String
  ): ZIO[UsersRepository, AppError, OutgoingUserUnauthenticated] =
    ZIO.serviceWithZIO[UsersRepository](_.getUserUnauthenticated(id))

  def updateUserIcon(
      username: String,
      iconSrc: String
  ): ZIO[UsersRepository, AppError, OutgoingUser] =
    ZIO.serviceWithZIO[UsersRepository](_.updateUserIcon(username, iconSrc))

  def updateUserBio(
      userId: String,
      bioInfo: UpdateUserBio
  ): ZIO[UsersRepository, AppError, OutgoingUser] =
    ZIO.serviceWithZIO[UsersRepository](_.updateUserBio(userId, bioInfo))

  def createUserTag(
      userId: String,
      tag: String
  ): ZIO[UsersRepository, AppError, OutgoingUser] =
    ZIO.serviceWithZIO[UsersRepository](_.createUserTag(userId, tag))

  def checkIfTagExists(tag: String): ZIO[UsersRepository, AppError, TagExists] =
    ZIO.serviceWithZIO[UsersRepository](_.checkIfTagExists(tag))

  def addOrRemoveCivility(
      userId: String,
      commentId: UUID,
      civility: Int,
      removeCivility: Boolean
  ): ZIO[UsersRepository, AppError, CivilityGivenResponse] =
    ZIO.serviceWithZIO[UsersRepository](
      _.addOrRemoveCivility(userId, commentId, civility, removeCivility)
    )

  import civil.repositories.QuillContext._

  def getUserInternal(
      userId: String
  ): ZIO[DataSource, AppError, Option[Users]] = {
    for {
      user <- run(query[Users].filter(u => u.userId == lift(userId)))
        .mapError(InternalServerError)
    } yield user.headOption
  }

}

case class UsersRepositoryLive(dataSource: DataSource) extends UsersRepository {

  import civil.repositories.QuillContext._

  private val profile_img_map = Map(
    "profile_img_1" -> "https://civil-dev.s3.us-west-1.amazonaws.com/profile_images/64_1.png",
    "profile_img_2" -> "https://civil-dev.s3.us-west-1.amazonaws.com/profile_images/64_2.png",
    "profile_img_3" -> "https://civil-dev.s3.us-west-1.amazonaws.com/profile_images/64_3.png",
    "profile_img_4" -> "https://civil-dev.s3.us-west-1.amazonaws.com/profile_images/64_4.png",
    "profile_img_5" -> "https://civil-dev.s3.us-west-1.amazonaws.com/profile_images/64_5.png",
    "profile_img_6" -> "https://civil-dev.s3.us-west-1.amazonaws.com/profile_images/64_6.png",
    "profile_img_7" -> "https://civil-dev.s3.us-west-1.amazonaws.com/profile_images/64_7.png",
    "profile_img_8" -> "https://civil-dev.s3.us-west-1.amazonaws.com/profile_images/64_8.png",
    "profile_img_9" -> "https://civil-dev.s3.us-west-1.amazonaws.com/profile_images/64_9.png",
    "profile_img_10" -> "https://civil-dev.s3.us-west-1.amazonaws.com/profile_images/64_10.png",
    "profile_img_11" -> "https://civil-dev.s3.us-west-1.amazonaws.com/profile_images/64_11.png",
    "profile_img_12" -> "https://civil-dev.s3.us-west-1.amazonaws.com/profile_images/64_12.png",
    "profile_img_13" -> "https://civil-dev.s3.us-west-1.amazonaws.com/profile_images/64_13.png",
    "profile_img_14" -> "https://civil-dev.s3.us-west-1.amazonaws.com/profile_images/64_14.png",
    "profile_img_15" -> "https://civil-dev.s3.us-west-1.amazonaws.com/profile_images/64_15.png",
    "profile_img_16" -> "https://civil-dev.s3.us-west-1.amazonaws.com/profile_images/64_16.png"
  )

  override def upsertDidUser(
      incomingUser: IncomingUser
  ): ZIO[Any, AppError, OutgoingUser] = {
    for {
      upsertedUser <- run(
        query[Users]
          .insertValue(
            lift(
              Users(
                incomingUser.userId,
                incomingUser.username,
                None,
                Some(
                  incomingUser.iconSrc.getOrElse(
                    profile_img_map.getOrElse("profile_img_1", "")
                  )
                ),
                13.60585f,
                ZonedDateTime.now(ZoneId.systemDefault()),
                false,
                None,
                None,
                true
              )
            )
          )
          .onConflictUpdate(_.userId)(
            (t, e) => t.username -> e.username,
            (t, e) => t.iconSrc -> e.iconSrc
          )
          .returning(u => u)
      )
        .mapError(DatabaseError(_))
        .provideEnvironment(ZEnvironment(dataSource))
    } yield upsertedUser
      .into[OutgoingUser]
      .withFieldConst(_.isFollowing, Some(false))
      .withFieldComputed(
        _.userLevelData,
        u => Some(UserLevel.apply(u.civility.toDouble))
      )
      .enableDefaultValues
      .transform

  }

  override def getUser(
      id: String,
      requesterId: String
  ): ZIO[Any, AppError, OutgoingUser] = {
    (for {
      userQuery <- run(query[Users].filter(u => u.userId == lift(id)))

      user <- ZIO
        .fromOption(userQuery.headOption)
        .orElseFail(DatabaseError(new Throwable("Could Not Locate User")))
      isFollowing <-
        run(
          query[Follows]
            .filter(f =>
              f.userId == lift(requesterId) && f.followedUserId == lift(id)
            )
            .nonEmpty
        )
      following <- run(
        query[Follows].filter(f => f.userId == lift(requesterId)).size
      )

      followed <-
        run(
          query[Follows].filter(f => f.followedUserId == lift(requesterId)).size
        )

      numPosts <- run(
        query[Spaces]
          .filter(t => t.createdByUserId == lift(requesterId))
          .map(_.createdByUserId) ++ query[Discussions]
          .filter(st =>
            st.createdByUserId == lift(requesterId) && st.title != "General"
          )
          .map(_.createdByUserId) ++ query[Comments]
          .filter(c => c.createdByUserId == lift(requesterId))
          .map(_.createdByUserId)
      )

    } yield user
      .into[OutgoingUser]
      .withFieldConst(_.isFollowing, Some(isFollowing))
      .withFieldConst(_.numFollowed, Some(following.toInt))
      .withFieldConst(_.numFollowers, Some(followed.toInt))
      .withFieldConst(_.numPosts, Some(numPosts.size))
      .withFieldComputed(
        _.userLevelData,
        u => {
          Some(UserLevel.apply(u.civility.toDouble))
        }
      )
      .enableDefaultValues
      .transform)
      .mapError(DatabaseError)
      .provideEnvironment(ZEnvironment(dataSource))
  }

  override def getUserUnauthenticated(
      id: String
  ): ZIO[Any, AppError, OutgoingUserUnauthenticated] = {
    (for {
      userQuery <- run(query[Users].filter(u => u.userId == lift(id)))

      user <- ZIO
        .fromOption(userQuery.headOption)
        .orElseFail(DatabaseError(new Throwable("Could Not Locate User")))

      following <- run(
        query[Follows].filter(f => f.userId == lift(id)).size
      )

      followed <-
        run(
          query[Follows].filter(f => f.followedUserId == lift(id)).size
        )

      numPosts <- run(
        query[Spaces]
          .filter(t => t.createdByUserId == lift(id))
          .map(_.createdByUserId) ++ query[Discussions]
          .filter(st => st.createdByUserId == lift(id) && st.title != "General")
          .map(_.createdByUserId) ++ query[Comments]
          .filter(c => c.createdByUserId == lift(id))
          .map(_.createdByUserId)
      )

    } yield user
      .into[OutgoingUserUnauthenticated]
      .withFieldConst(_.numFollowed, Some(following.toInt))
      .withFieldConst(_.numFollowers, Some(followed.toInt))
      .withFieldConst(_.numPosts, Some(numPosts.size))
      .withFieldComputed(
        _.userLevelData,
        u => {
          Some(UserLevel.apply(u.civility.toDouble))
        }
      )
      .enableDefaultValues
      .transform)
      .mapError(DatabaseError)
      .provideEnvironment(ZEnvironment(dataSource))

  }

  override def updateUserIcon(
      username: String,
      iconSrc: String
  ): ZIO[Any, AppError, OutgoingUser] = {
    for {
      user <-
        run(
          query[Users]
            .filter(u => u.username == lift(username))
            .update(user => user.iconSrc -> lift(Option(iconSrc)))
            .returning(r => r)
        )
          .mapError(DatabaseError(_))
          .provideEnvironment(ZEnvironment(dataSource))
    } yield user
      .into[OutgoingUser]
      .withFieldConst(_.isFollowing, Some(false))
      .withFieldComputed(
        _.userLevelData,
        u => Some(UserLevel.apply(u.civility.toDouble))
      )
      .enableDefaultValues
      .transform

  }

  override def updateUserBio(
      userId: String,
      bioInfo: UpdateUserBio
  ): ZIO[Any, AppError, OutgoingUser] = {

    for {
      user <-
        run(
          query[Users]
            .filter(u => u.userId == lift(userId))
            .update(
              _.bio -> lift(bioInfo.bio),
              _.experience -> lift(bioInfo.experience)
            )
            .returning(u => u)
        )
          .mapError(DatabaseError(_))
          .provideEnvironment(ZEnvironment(dataSource))
    } yield user
      .into[OutgoingUser]
      .withFieldConst(_.isFollowing, Some(false))
      .withFieldComputed(
        _.userLevelData,
        u => Some(UserLevel.apply(u.civility.toDouble))
      )
      .enableDefaultValues
      .transform
  }

  override def addOrRemoveCivility(
      userId: String,
      commentId: UUID,
      civility: Int,
      removeCivility: Boolean
  ): ZIO[Any, AppError, CivilityGivenResponse] = {

    for {
      commentQuery <- run(query[Comments].filter(c => c.id == lift(commentId)))
        .mapError(DatabaseError(_))
        .provideEnvironment(ZEnvironment(dataSource))
      comment <- ZIO
        .fromOption(commentQuery.headOption)
        .orElseFail(
          DatabaseError(
            new Throwable(
              "Service: UsersRepo -> addOrRemoveCivility - Could Not find comment"
            )
          )
        )
        .provideEnvironment(ZEnvironment(dataSource))
      res <- run(
        query[Users]
          .filter(u => u.userId == lift(userId))
          .update(user => user.civility -> (user.civility + lift(civility)))
          .returning(user =>
            CivilityGivenResponse(
              user.civility,
              lift(commentId),
              lift(comment.rootId)
            )
          )
      ).mapError(DatabaseError(_)).provideEnvironment(ZEnvironment(dataSource))
    } yield res
  }

  override def createUserTag(
      userId: String,
      tag: String
  ): ZIO[Any, AppError, OutgoingUser] = {

    for {
      user <- run(
        query[Users]
          .filter(u => u.userId == lift(userId) && u.tag.isEmpty)
          .update(_.tag -> lift(Option(tag)))
          .returning(u => u)
      ).mapError(_ =>
        DatabaseError(new Throwable("Cannot Update Tag More Than Once"))
      ).provideEnvironment(ZEnvironment(dataSource))
    } yield user
      .into[OutgoingUser]
      .withFieldConst(_.isFollowing, None)
      .withFieldComputed(
        _.userLevelData,
        u => Some(UserLevel.apply(u.civility.toDouble))
      )
      .enableDefaultValues
      .transform
  }

  override def checkIfTagExists(tag: String): ZIO[Any, AppError, TagExists] = {
    for {
      userQuery <- run(
        query[Users].filter(u => u.tag == lift(Option(tag)))
      ).mapError(DatabaseError(_)).provideEnvironment(ZEnvironment(dataSource))
    } yield TagExists(tagExists = userQuery.nonEmpty)
  }
}

object UsersRepositoryLive {
  val layer: URLayer[DataSource, UsersRepository] =
    ZLayer.fromFunction(UsersRepositoryLive.apply _)
}
