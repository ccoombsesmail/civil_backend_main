package civil.repositories.spaces

import civil.errors.AppError
import civil.errors.AppError.{DatabaseError, InternalServerError}
import civil.models._
import civil.directives.OutgoingHttp
import civil.models.NotifcationEvents.SpaceMLEvent
import civil.models.actions.{LikeAction, NeutralState}
import civil.models.enums.SpaceCategories
import civil.repositories.recommendations.RecommendationsRepository
import civil.services.{KafkaProducerService, KafkaProducerServiceLive}
import io.scalaland.chimney.dsl._
import zio.{ZIO, _}
import io.getquill._

import java.util.UUID
import javax.sql.DataSource
import scala.concurrent.ExecutionContext

case class SpaceWithLinkData(
    space: Spaces,
    externalLinks: Option[ExternalLinks]
)

case class SpaceRepoHelpers(
    recommendationsRepository: RecommendationsRepository,
    dataSource: DataSource
) {

  import civil.repositories.QuillContext._

  implicit val ec: ExecutionContext = ExecutionContext.global

  private def spacesUsersVodsLinksJoin(
      fromUserId: Option[String]
  ): Quoted[Query[
    (
        Spaces,
        Users,
        Option[SpaceFollows]
    )
  ]] = {
    fromUserId match {
      case Some(userId) =>
        quote {
          query[Spaces]
            .filter(_.createdByUserId == lift(userId))
            .join(query[Users])
            .on(_.createdByUserId == _.userId)
            .leftJoin(query[SpaceFollows])
            .on { case ((t, u), tf) =>
              t.id == tf.followedSpaceId && u.userId == tf.userId
            }
            .map { case ((t, u), tf) => (t, u, tf) }
        }
      case None =>
        quote {
          query[Spaces]
            //            .filter(t => liftQuery(spaceIds).contains(t.id))
            .join(query[Users])
            .on(_.createdByUserId == _.userId)
            .leftJoin(query[SpaceFollows])
            .on { case ((t, u), tf) =>
              t.id == tf.followedSpaceId && u.userId == tf.userId
            }
            .map { case ((t, u), tf) => (t, u, tf) }
        }
    }

  }

  def getDefaultDiscussion(space: Spaces): Discussions = Discussions(
    id = UUID.randomUUID(),
    spaceId = space.id,
    createdByUsername = space.createdByUsername,
    title = "General",
    createdAt = space.createdAt,
    createdByUserId = space.createdByUserId,
    likes = 0,
    evidenceLinks = None,
    editorState = "General Discussion",
    editorTextContent = "General, Discussion",
    userUploadedVodUrl = None,
    userUploadedImageUrl = None,
    contentHeight = None,
    popularityScore = 0.0
  )

  def getSpacesWithLikeStatus(
      requestingUserID: String,
      fromUserId: Option[String] = None,
      skip: Int = 0
  ): ZIO[Any, AppError, List[OutgoingSpace]] =
    (for {
      likes <- run(query[SpaceLikes].filter(_.userId == lift(requestingUserID)))
      likesMap = likes.foldLeft(Map[UUID, LikeAction]()) { (m, t) =>
        m + (t.spaceId -> t.likeState)
      }

      spacesUsersVodsJoin <- run(
        spacesUsersVodsLinksJoin(fromUserId)
          .drop(lift(skip))
          .take(5)
          .sortBy(_._1.createdAt)(Ord.desc)
      )
      outgoingSpaces <- ZIO
        .foreachPar(spacesUsersVodsJoin)(row => {
          val (space, user, spaceFollow) = row
          ZIO
            .attempt(
              space
                .into[OutgoingSpace]
                .withFieldConst(_.createdByIconSrc, user.iconSrc.get)
                .withFieldConst(
                  _.likeState,
                  likesMap.getOrElse(space.id, NeutralState)
                )
                .withFieldConst(_.createdByTag, user.tag)
                .withFieldConst(_.isFollowing, spaceFollow.isDefined)
                .withFieldComputed(_.editorState, row => row.editorState)
                .withFieldComputed(
                  _.category,
                  row => SpaceCategories.withName(row.category)
                )
                .transform
            )
            .mapError(DatabaseError)
        })
        .withParallelism(10)
    } yield outgoingSpaces)
      .mapError(DatabaseError)
      .provideEnvironment(ZEnvironment(dataSource))

}

trait SpacesRepository {
  def insertSpace(
      space: Spaces
  ): ZIO[Any, AppError, OutgoingSpace]

  def getSpaces: ZIO[Any, AppError, List[OutgoingSpace]]

  def getSpacesAuthenticated(
      requestingUserID: String,
      userData: JwtUserClaimsData,
      skip: Int
  ): ZIO[Any, AppError, List[OutgoingSpace]]

  def getSpace(
      id: UUID,
      requestingUserID: String
  ): ZIO[Any, AppError, OutgoingSpace]

  def getUserSpaces(
      requestingUserId: String,
      userId: String
  ): ZIO[Any, AppError, List[OutgoingSpace]]

  def getFollowedSpaces(
      requestingUserId: String
  ): ZIO[Any, AppError, List[OutgoingSpace]]

  def getSimilarSpaces(
      spaceId: UUID
  ): ZIO[Any, AppError, List[OutgoingSpace]]

}

object SpacesRepository {
  def insertSpace(
      space: Spaces
  ): ZIO[SpacesRepository, AppError, OutgoingSpace] =
    ZIO.serviceWithZIO[SpacesRepository](
      _.insertSpace(space)
    )

  def getSpaces: ZIO[SpacesRepository, AppError, List[OutgoingSpace]] =
    ZIO.serviceWithZIO[SpacesRepository](_.getSpaces)

  def getSpacesAuthenticated(
      requestingUserID: String,
      userData: JwtUserClaimsData,
      skip: Int
  ): ZIO[SpacesRepository, AppError, List[OutgoingSpace]] =
    ZIO.serviceWithZIO[SpacesRepository](
      _.getSpacesAuthenticated(requestingUserID, userData, skip)
    )

  def getSpace(
      id: UUID,
      requestingUserID: String
  ): ZIO[SpacesRepository, AppError, OutgoingSpace] =
    ZIO.serviceWithZIO[SpacesRepository](_.getSpace(id, requestingUserID))

  def getUserSpaces(
      requestingUserId: String,
      userId: String
  ): ZIO[SpacesRepository, AppError, List[OutgoingSpace]] =
    ZIO.serviceWithZIO[SpacesRepository](
      _.getUserSpaces(requestingUserId, userId)
    )

  def getFollowedSpaces(
      requestingUserId: String
  ): ZIO[SpacesRepository, AppError, List[OutgoingSpace]] =
    ZIO.serviceWithZIO[SpacesRepository](_.getFollowedSpaces(requestingUserId))

  def getSimilarSpaces(
      spaceId: UUID
  ): ZIO[SpacesRepository, AppError, List[OutgoingSpace]] =
    ZIO.serviceWithZIO[SpacesRepository](
      _.getSimilarSpaces(spaceId)
    )
}

case class SpaceRepositoryLive(
    recommendationsRepository: RecommendationsRepository,
    dataSource: DataSource
) extends SpacesRepository {

  private val helpers = SpaceRepoHelpers(recommendationsRepository, dataSource)

  import helpers._
  import civil.repositories.QuillContext._

  val kafka = new KafkaProducerServiceLive()

  override def insertSpace(
      incomingSpace: Spaces
  ): ZIO[Any, AppError, OutgoingSpace] = {
    (for {
      userQuery <-
        run(
          query[Users].filter(u =>
            u.userId == lift(incomingSpace.createdByUserId)
          )
        )
      _ = println(incomingSpace)

      user <- ZIO
        .fromOption(userQuery.headOption)
        .orElseFail(DatabaseError(new Throwable("Can't find user")))
      _ <- transaction {
        for {
          inserted <- run(
            query[Spaces]
              .insertValue(lift(incomingSpace))
              .returning(inserted => inserted)
          )
          _ <- run(
            query[Discussions].insertValue(lift(getDefaultDiscussion(inserted)))
          )
        } yield ()
      }
      _ <- kafka.publish(
        SpaceMLEvent(
          eventType = "SpaceMLEvent",
          spaceId = incomingSpace.id,
          editorTextContent =
            s"${incomingSpace.title}: ${incomingSpace.editorTextContent}"
        ),
        incomingSpace.id.toString,
        SpaceMLEvent.spaceMLEventSerde,
        "ml-pipeline"
      )

      outgoingSpace =
        incomingSpace
          .into[OutgoingSpace]
          .withFieldConst(_.createdByIconSrc, user.iconSrc.getOrElse(""))
          .withFieldConst(_.likeState, NeutralState)
          .withFieldConst(_.createdByTag, user.tag)
          .withFieldConst(_.isFollowing, false)
          .withFieldComputed(
            _.category,
            row => SpaceCategories.withName(row.category)
          )
          .transform
    } yield outgoingSpace)
      .mapError(DatabaseError(_))
      .provideEnvironment(ZEnvironment(dataSource))

  }

  override def getSpaces: ZIO[Any, AppError, List[OutgoingSpace]] = {

    val joined = quote {
      query[Spaces]
        .join(query[Users])
        .on(_.createdByUserId == _.userId)
    }

    for {
      likes <- run(query[SpaceLikes])
        .mapError(DatabaseError(_))
        .provideEnvironment(ZEnvironment(dataSource))
      likesMap = likes.foldLeft(Map[UUID, LikeAction]()) { (m, t) =>
        m + (t.spaceId -> t.likeState)
      }
      joinedVals <- run(joined)
        .mapError(DatabaseError(_))
        .provideEnvironment(ZEnvironment(dataSource))
      spacesUsersVodsLinksJoin = joinedVals.map { case (t, u) =>
        (t, u)
      }

      outgoingSpaces <- ZIO.foreach(spacesUsersVodsLinksJoin)(row => {
        val (space, user) = row
        ZIO
          .attempt(
            space
              .into[OutgoingSpace]
              .withFieldConst(_.createdByIconSrc, user.iconSrc.get)
              .withFieldConst(
                _.likeState,
                likesMap.getOrElse(space.id, NeutralState)
              )
              .withFieldConst(_.createdByTag, user.tag)
              .withFieldComputed(
                _.category,
                row => SpaceCategories.withName(row.category)
              )
              .enableDefaultValues
              .transform
          )
          .mapError(DatabaseError(_))
      })
    } yield outgoingSpaces.sortWith((t1, t2) =>
      t2.createdAt.isBefore(t1.createdAt)
    )

  }

  override def getSpace(
      id: UUID,
      requestingUserID: String
  ): ZIO[Any, AppError, OutgoingSpace] = {

    val joined = quote {
      query[Spaces]
        .filter(t => t.id == lift(id))
        .join(query[Users])
        .on(_.createdByUserId == _.userId)
        .leftJoin(query[SpaceFollows])
        .on { case ((t, u), tf) => t.id == tf.followedSpaceId }
        .map { case ((t, u), tf) => (t, u, tf) }

    }

    for {
      likes <- run(
        query[SpaceLikes].filter(l =>
          l.userId == lift(requestingUserID) && l.spaceId == lift(id)
        )
      )
        .mapError(DatabaseError(_))
        .provideEnvironment(ZEnvironment(dataSource))
      likesMap = likes.foldLeft(Map[UUID, LikeAction]()) { (m, t) =>
        m + (t.spaceId -> t.likeState)
      }
      joinedVals <- run(joined)
        .mapError(DatabaseError(_))
        .provideEnvironment(ZEnvironment(dataSource))
      _ <- ZIO
        .fail(DatabaseError(new Throwable("Can't find space details")))
        .unless(joinedVals.nonEmpty)
    } yield joinedVals
      .map(row => {
        val (space, user, spaceFollow) = row

        space
          .into[OutgoingSpace]
          .withFieldConst(_.createdByIconSrc, user.iconSrc.get)
          .withFieldConst(
            _.likeState,
            likesMap.getOrElse(space.id, NeutralState)
          )
          .withFieldConst(_.createdByTag, user.tag)
          .withFieldConst(_.isFollowing, spaceFollow.isDefined)
          .withFieldComputed(
            _.category,
            row => SpaceCategories.withName(row.category)
          )
          .transform
      })
      .head

  }

  override def getSpacesAuthenticated(
      requestingUserID: String,
      userData: JwtUserClaimsData,
      skip: Int
  ): ZIO[Any, AppError, List[OutgoingSpace]] = {
    //    val spaceIds = run(
    //      query[ForYouSpaces].filter(_.userId == requestingUserID)
    //    ).head
    getSpacesWithLikeStatus(requestingUserID, None, skip)
  }

  override def getUserSpaces(
      requestingUserId: String,
      userId: String
  ): ZIO[Any, AppError, List[OutgoingSpace]] = {
    getSpacesWithLikeStatus(requestingUserId, Some(userId), 0)

  }

  override def getFollowedSpaces(
      requestingUserId: String
  ): ZIO[Any, AppError, List[OutgoingSpace]] = {
    (for {
      likes <- run(query[SpaceLikes].filter(_.userId == lift(requestingUserId)))
      likesMap = likes.foldLeft(Map[UUID, LikeAction]()) { (m, t) =>
        m + (t.spaceId -> t.likeState)
      }
      spacesUsersVodsJoin <- run(
        query[Spaces]
          .join(query[Users])
          .on(_.createdByUserId == _.userId)
          .leftJoin(query[SpaceFollows])
          .on { case ((t, u), sf) => t.id == sf.followedSpaceId }
          .filter { case ((t, u), sf) =>
            sf.exists(_.userId == lift(requestingUserId))
          }
          .map { case ((t, u), tf) => (t, u, tf) }
          .sortBy(_._1.createdAt)(Ord.desc)
      )
      outgoingSpaces <- ZIO
        .foreachPar(spacesUsersVodsJoin)(row => {
          val (space, user, spaceFollow) = row
          ZIO
            .attempt(
              space
                .into[OutgoingSpace]
                .withFieldConst(_.createdByIconSrc, user.iconSrc.get)
                .withFieldConst(
                  _.likeState,
                  likesMap.getOrElse(space.id, NeutralState)
                )
                .withFieldConst(_.createdByTag, user.tag)
                .withFieldConst(_.isFollowing, spaceFollow.isDefined)
                .withFieldComputed(_.editorState, row => row.editorState)
                .withFieldComputed(
                  _.category,
                  row => SpaceCategories.withName(row.category)
                )
                .transform
            )
            .mapError(DatabaseError)
        })
        .withParallelism(10)

    } yield outgoingSpaces)
      .mapError(DatabaseError)
      .provideEnvironment(ZEnvironment(dataSource))

  }

  override def getSimilarSpaces(
      spaceId: UUID
  ): ZIO[Any, AppError, List[OutgoingSpace]] = {

    for {
      spacesWithUser <- run(
        query[Spaces]
          .join(query[Users])
          .on(_.createdByUserId == _.userId)
          .join(query[SpaceSimilarityScores])
          .on { case ((s, u), sss) =>
            (sss.spaceId1 == lift(
              spaceId
            ) && s.id == sss.spaceId2) || (sss.spaceId2 == lift(
              spaceId
            ) && s.id == sss.spaceId1)
          }
          .filter { case ((s, u), sss) => s.id != lift(spaceId) }
          .sortBy(_._2.similarityScore)(Ord.desc)
          .take(30)
          .map(_._1)
      ).mapError(DatabaseError(_))
        .provideEnvironment(ZEnvironment(dataSource))
      outgoingSpaces <- ZIO.foreachPar(spacesWithUser)(row => {
        val (space, user) = row
        ZIO
          .attempt(
            space
              .into[OutgoingSpace]
              .withFieldConst(_.createdByIconSrc, user.iconSrc.get)
              .withFieldConst(
                _.likeState,
                NeutralState
              )
              .withFieldConst(_.createdByTag, user.tag)
              .withFieldConst(_.isFollowing, false)
              .withFieldComputed(
                _.category,
                row => SpaceCategories.withName(row.category)
              )
              .transform
          )
          .mapError(DatabaseError(_))
          .provideEnvironment(ZEnvironment(dataSource))
      })
    } yield outgoingSpaces

  }

}

object SpaceRepositoryLive {

  val layer: URLayer[
    DataSource with RecommendationsRepository,
    SpacesRepository,
  ] = ZLayer.fromFunction(SpaceRepositoryLive.apply _)
}
