package civil.repositories

import civil.models.enums.ClerkEventType
import civil.models._
import io.scalaland.chimney.dsl._
import zio._

import java.time.{LocalDateTime, ZoneId}
import java.util.UUID

trait UsersRepository {
  def upsertDidUser(
      incomingUser: IncomingUser
  ): ZIO[Any, ErrorInfo, OutgoingUser]
  def insertOrUpdateUserHook(
      webHookData: WebHookData,
      eventType: ClerkEventType
  ): ZIO[Any, ErrorInfo, Unit]
  def getUser(
      id: String,
      requesterId: String
  ): ZIO[Any, ErrorInfo, OutgoingUser]
  def updateUserIcon(
      username: String,
      iconSrc: String
  ): ZIO[Any, ErrorInfo, OutgoingUser]
  def updateUserBio(
      userId: String,
      bioInfo: UpdateUserBio
  ): ZIO[Any, ErrorInfo, OutgoingUser]

  def addOrRemoveCivility(
      userId: String,
      commentId: UUID,
      civility: Int,
      removeCivility: Boolean
  ): Task[CivilityGiven]

  def createUserTag(userId: String, tag: String): ZIO[Any, ErrorInfo, OutgoingUser]
  def checkIfTagExists(tag: String): ZIO[Any, ErrorInfo, TagExists]

}

object UsersRepository {
  def upsertDidUser(
      incomingUser: IncomingUser
  ): ZIO[Has[UsersRepository], ErrorInfo, OutgoingUser] =
    ZIO.serviceWith[UsersRepository](_.upsertDidUser(incomingUser))

  def insertOrUpdateUserHook(
      webHookData: WebHookData,
      eventType: ClerkEventType
  ): ZIO[Has[UsersRepository], ErrorInfo, Unit] =
    ZIO.serviceWith[UsersRepository](
      _.insertOrUpdateUserHook(webHookData, eventType)
    )

  def getUser(
      id: String,
      requesterId: String
  ): ZIO[Has[UsersRepository], ErrorInfo, OutgoingUser] =
    ZIO.serviceWith[UsersRepository](_.getUser(id, requesterId))

  def updateUserIcon(
      username: String,
      iconSrc: String
  ): ZIO[Has[UsersRepository], ErrorInfo, OutgoingUser] =
    ZIO.serviceWith[UsersRepository](_.updateUserIcon(username, iconSrc))

  def updateUserBio(
      userId: String,
      bioInfo: UpdateUserBio
  ): ZIO[Has[UsersRepository], ErrorInfo, OutgoingUser] =
    ZIO.serviceWith[UsersRepository](_.updateUserBio(userId, bioInfo))

  def createUserTag(
      userId: String,
      tag: String
  ): ZIO[Has[UsersRepository], ErrorInfo, OutgoingUser] =
    ZIO.serviceWith[UsersRepository](_.createUserTag(userId, tag))

  def checkIfTagExists(tag: String): ZIO[Has[UsersRepository], ErrorInfo, TagExists] =
    ZIO.serviceWith[UsersRepository](_.checkIfTagExists(tag))

  def addOrRemoveCivility(
      userId: String,
      commentId: UUID,
      civility: Int,
      removeCivility: Boolean
  ): RIO[Has[UsersRepository], CivilityGiven] =
    ZIO.serviceWith[UsersRepository](
      _.addOrRemoveCivility(userId, commentId, civility, removeCivility)
    )

  import QuillContextHelper.ctx._

  def getUserInternal(userId: String): Option[Users] = {
    val q = quote {
      query[Users].filter(u => u.userId == lift(userId))
    }
    run(q).headOption
  }

}

case class UsersRepositoryLive() extends UsersRepository {
  import QuillContextHelper.ctx._

  val profile_img_map = Map(
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
  ): ZIO[Any, ErrorInfo, OutgoingUser] = {
    for {
      upsertedUser <- ZIO
        .effect(
          run(
            query[Users]
              .insert(
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
                    LocalDateTime
                      .now(),
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
        )
        .mapError(e => InternalServerError(e.getMessage))
    } yield upsertedUser
      .into[OutgoingUser]
      .withFieldConst(_.isFollowing, Some(false))
      .withFieldComputed(_.userLevelData, u => Some(UserLevel.apply(u.civility.toDouble)))
      .transform

  }

  override def insertOrUpdateUserHook(
      webHookData: WebHookData,
      eventType: ClerkEventType
  ): ZIO[Any, ErrorInfo, Unit] = {
    for {
      _ <- ZIO
        .when(eventType == ClerkEventType.UserCreated)(
          ZIO.effect(
            run(
              query[Users].insert(
                lift(
                  Users(
                    webHookData.id,
                    webHookData.username.get,
                    None,
                    Some(webHookData.profile_image_url),
                    13.60585f,
                    LocalDateTime.now(),
                    false,
                    None,
                    None,
                    false
                  )
                )
              )
            )
          )
        )
        .mapError(e =>
          InternalServerError(e.toString)
        )
      username = webHookData.username.get
      _ = println(webHookData.unsafe_metadata)
      _ <- ZIO
        .when(eventType == ClerkEventType.UserUpdated)(
          ZIO.effect(
            run(
              query[Users]
                .filter(u => u.userId == lift(webHookData.id))
                .update(
                  _.iconSrc -> lift(Option(webHookData.profile_image_url)),
                  _.username -> lift(username),
                  _.tag -> lift(webHookData.unsafe_metadata.userCivilTag)
                )
                .returning(r => r)
            )
          )
        )
        .mapError(e => InternalServerError(e.toString))

    } yield ()

  }

  override def getUser(
      id: String,
      requesterId: String
  ): ZIO[Any, ErrorInfo, OutgoingUser] = {
    for {
      user <- ZIO
        .fromOption(
          run(query[Users].filter(u => u.userId == lift(id))).headOption
        )
        .orElseFail(NotFound("Couldn't locate user info"))
      isFollowing <- ZIO
        .effect(
          run(
            query[Follows].filter(f =>
              f.userId == lift(requesterId) && f.followedUserId == lift(id)
            ).nonEmpty
        ))
        .mapError(e => InternalServerError(e.toString))
      following <- ZIO
        .effect(
          run(
            query[Follows].filter(f =>
              f.userId == lift(requesterId)
            ).size
          ))
        .mapError(e => InternalServerError(e.toString))
      followed <- ZIO
        .effect(
          run(
            query[Follows].filter(f =>
              f.followedUserId == lift(requesterId)
            ).size
          ))
        .mapError(e => InternalServerError(e.toString))
      numPosts <- ZIO
        .effect(
          run(
            query[Topics].filter(t =>
              t.createdByUserId == lift(requesterId)
            ).map(_.createdByUserId) ++ query[Discussions].filter(st =>
              st.createdByUserId == lift(requesterId) && st.title != "General"
            ).map(_.createdByUserId) ++ query[Comments].filter(c =>
            c.createdByUserId == lift(requesterId)
            ).map(_.createdByUserId)
          ).size)
        .mapError(e => InternalServerError(e.toString))
    } yield user
      .into[OutgoingUser]
      .withFieldConst(_.isFollowing, Some(isFollowing))
      .withFieldConst(_.numFollowed, Some(following.toInt))
      .withFieldConst(_.numFollowers, Some(followed.toInt))
      .withFieldConst(_.numPosts, Some(numPosts))
      .withFieldComputed(_.userLevelData, u => {
        println(Some(UserLevel.apply(u.civility)))
        Some(UserLevel.apply(u.civility.toDouble))
        })
      .transform
  }

  override def updateUserIcon(
      username: String,
      iconSrc: String
  ): ZIO[Any, ErrorInfo, OutgoingUser] = {
    for {
      user <- ZIO
        .effect(
          run(
            query[Users]
              .filter(u => u.username == lift(username))
              .update(user => user.iconSrc -> lift(Option(iconSrc)))
              .returning(r => r)
          )
        )
        .mapError(e => InternalServerError(e.toString))
    } yield user
      .into[OutgoingUser]
      .withFieldConst(_.isFollowing, Some(false))
      .withFieldComputed(_.userLevelData, u => Some(UserLevel.apply(u.civility.toDouble)))
      .transform

  }

  override def updateUserBio(
      userId: String,
      bioInfo: UpdateUserBio
  ): ZIO[Any, ErrorInfo, OutgoingUser] = {

    for {
      user <- ZIO
        .effect(
          run(
            query[Users]
              .filter(u => u.userId == lift(userId))
              .update(
                _.bio -> lift(bioInfo.bio),
                _.experience -> lift(bioInfo.experience)
              )
              .returning(u => u)
          )
        )
        .mapError(e => InternalServerError(e.toString))
    } yield user
      .into[OutgoingUser]
      .withFieldConst(_.isFollowing, Some(false))
      .withFieldComputed(_.userLevelData, u => Some(UserLevel.apply(u.civility.toDouble)))
      .transform
  }

  override def addOrRemoveCivility(
      userId: String,
      commentId: UUID,
      civility: Int,
      removeCivility: Boolean
  ): Task[CivilityGiven] = {

    val commentCivilityStatus = removeCivility match {
      case true  => None
      case false => Some(civility == 1)
    }

    val comment = run(query[Comments].filter(c => c.id == lift(commentId))).head

    val q = quote {
      query[Users]
        .filter(u => u.userId == lift(userId))
        .update(user => user.civility -> (user.civility + lift(civility)))
        .returning(user =>
          CivilityGiven(
            user.civility,
            lift(commentId),
            lift(comment.rootId)
          )
        )
    }
    val c = run(q)
    ZIO.succeed(c)
  }

  override def createUserTag(userId: String, tag: String): ZIO[Any, ErrorInfo, OutgoingUser] = {

    for {
      user <- ZIO.effect(run(
        query[Users]
          .filter(u => u.userId == lift(userId) && u.tag.isEmpty)
          .update(_.tag -> lift(Option(tag)))
          .returning(u => u)
      )).mapError(_ => BadRequest("Cannot Update Tag More Than Once"))
    } yield user.into[OutgoingUser]
      .withFieldConst(_.isFollowing, None)
      .withFieldComputed(_.userLevelData, u => Some(UserLevel.apply(u.civility.toDouble)))
      .transform
  }

  override def checkIfTagExists(tag: String): ZIO[Any, ErrorInfo, TagExists] = {
    for {
      user <- ZIO.effect(run(
        query[Users].filter(u => u.tag == lift(Option(tag)))
      ).headOption).mapError(e => BadRequest(e.toString))
    } yield TagExists(tagExists=user.isDefined)
  }
}

object UsersRepositoryLive {
  val live: ZLayer[Any, Throwable, Has[UsersRepository]] =
    ZLayer.succeed(UsersRepositoryLive())

}
