package civil.repositories.discussions

import cats.implicits.catsSyntaxOptionId
import civil.errors.AppError
import civil.errors.AppError.{DatabaseError, InternalServerError}
import civil.models.NotifcationEvents.DiscussionMLEvent
import civil.models.{Users, _}
import civil.models.actions.{LikeAction, NeutralState}
import civil.models.enums.LinkType.Web
import civil.models.enums.{ReportStatus, SpaceCategories}
import civil.repositories.DiscussionQueries.{
  DiscussionsData,
  getAllFollowedDiscussions,
  getAllPopularDiscussions,
  getAllUserDiscussions,
  getOneDiscussion,
  getSimilarDiscussionsQuery,
  getSpaceDiscussionsQuery
}
import civil.services.KafkaProducerServiceLive
import io.getquill.Ord
import io.scalaland.chimney.dsl._
import zio._

import java.util.UUID
import javax.sql.DataSource

trait DiscussionRepository {
  def insertDiscussion(
      discussion: Discussions,
      linkData: Option[ExternalLinksDiscussions]
  ): ZIO[Any, AppError, Discussions]

  def getSpaceDiscussions(
      spaceId: UUID,
      skip: Int,
      userId: String
  ): ZIO[Any, AppError, List[OutgoingDiscussion]]

  def getDiscussion(
      id: UUID,
      userId: String
  ): ZIO[Any, AppError, OutgoingDiscussion]

  def getGeneralDiscussionId(
      spaceId: UUID
  ): ZIO[Any, AppError, GeneralDiscussionId]

  def getUserDiscussions(
      requestingUserId: String,
      userId: String,
      skip: Int
  ): ZIO[Any, AppError, List[OutgoingDiscussion]]

  def getSimilarDiscussions(
      discussionId: UUID
  ): ZIO[Any, AppError, List[OutgoingDiscussion]]

  def getPopularDiscussions(
      userId: String,
      skip: Int
  ): ZIO[Any, AppError, List[OutgoingDiscussion]]

  def getFollowedDiscussions(
      requestingUserId: String,
      skip: Int
  ): ZIO[Any, AppError, List[OutgoingDiscussion]]
}

object DiscussionRepository {
  def insertDiscussion(
      discussion: Discussions,
      linkData: Option[ExternalLinksDiscussions]
  ): ZIO[DiscussionRepository, AppError, Discussions] =
    ZIO.serviceWithZIO[DiscussionRepository](
      _.insertDiscussion(discussion, linkData)
    )

  def getSpaceDiscussions(
      spaceId: UUID,
      skip: Int,
      userId: String
  ): ZIO[DiscussionRepository, AppError, List[OutgoingDiscussion]] =
    ZIO.serviceWithZIO[DiscussionRepository](
      _.getSpaceDiscussions(spaceId, skip, userId)
    )

  def getDiscussion(
      id: UUID,
      userId: String
  ): ZIO[DiscussionRepository, AppError, OutgoingDiscussion] =
    ZIO.serviceWithZIO[DiscussionRepository](_.getDiscussion(id, userId))

  def getGeneralDiscussionId(
      spaceId: UUID
  ): ZIO[DiscussionRepository, AppError, GeneralDiscussionId] =
    ZIO.serviceWithZIO[DiscussionRepository](_.getGeneralDiscussionId(spaceId))

  def getUserDiscussions(
      requestingUserId: String,
      userId: String,
      skip: Int
  ): ZIO[DiscussionRepository, AppError, List[OutgoingDiscussion]] =
    ZIO.serviceWithZIO[DiscussionRepository](
      _.getUserDiscussions(requestingUserId, userId, skip)
    )

  def getSimilarDiscussions(
      discussionId: UUID
  ): ZIO[DiscussionRepository, AppError, List[OutgoingDiscussion]] =
    ZIO.serviceWithZIO[DiscussionRepository](
      _.getSimilarDiscussions(discussionId)
    )

  def getPopularDiscussions(
      userId: String,
      skip: Int
  ): ZIO[DiscussionRepository, AppError, List[OutgoingDiscussion]] =
    ZIO.serviceWithZIO[DiscussionRepository](
      _.getPopularDiscussions(userId, skip)
    )

  def getFollowedDiscussions(
      requestingUserId: String,
      skip: Int
  ): ZIO[DiscussionRepository, AppError, List[OutgoingDiscussion]] =
    ZIO.serviceWithZIO[DiscussionRepository](
      _.getFollowedDiscussions(requestingUserId, skip)
    )
}

case class DiscussionRepositoryLive(dataSource: DataSource)
    extends DiscussionRepository {

  import civil.repositories.QuillContext._

  val kafka = new KafkaProducerServiceLive()

  override def insertDiscussion(
      discussion: Discussions,
      externalLinks: Option[ExternalLinksDiscussions]
  ): ZIO[Any, AppError, Discussions] = {

    for {
      _ <- ZIO
        .when(externalLinks.isEmpty)(
          run(
            query[Discussions]
              .insertValue(lift(discussion))
              .returning(inserted => inserted)
          )
        )
        .mapError(DatabaseError(_))
        .provideEnvironment(ZEnvironment(dataSource))
      _ <- ZIO
        .when(externalLinks.isDefined)(transaction {
          for {
            inserted <- run(
              query[Discussions]
                .insertValue(lift(discussion))
                .returning(inserted => inserted)
            )
            _ <- run(
              query[ExternalLinksDiscussions]
                .insertValue(
                  lift(externalLinks.get.copy(discussionId = inserted.id))
                )
            )
          } yield ()
        })
        .mapError(DatabaseError)
        .provideEnvironment(ZEnvironment(dataSource))

      _ <- kafka
        .publish(
          DiscussionMLEvent(
            eventType = "DiscussionMLEvent",
            discussionId = discussion.id,
            editorTextContent =
              s"${discussion.title}: ${discussion.editorTextContent}",
            externalLinks
          ),
          discussion.id.toString,
          DiscussionMLEvent.discussionMLEventSerde,
          "ml-pipeline"
        )
        .mapError(DatabaseError)
    } yield discussion
  }

  override def getSpaceDiscussions(
      spaceId: UUID,
      skip: Int,
      requestingUserId: String
  ): ZIO[Any, AppError, List[OutgoingDiscussion]] = {

    (for {

      discussionsJoin <- run(
        getSpaceDiscussionsQuery(
          lift(requestingUserId),
          lift(skip),
          lift(spaceId)
        )
      )
      discussions = discussionsJoin.map { row =>
        prepareOutgoingDiscussionRow(row)
      }
    } yield discussions)
      .mapError(DatabaseError(_))
      .provideEnvironment(ZEnvironment(dataSource))

  }

  override def getDiscussion(
      id: UUID,
      requestingUserId: String
  ): ZIO[Any, AppError, OutgoingDiscussion] = {
    (for {
      discussionsUsersLinksJoin <- run(
        getOneDiscussion(lift(requestingUserId), lift(id))
      )
      discussion <- ZIO
        .fromOption(discussionsUsersLinksJoin.headOption)
        .orElseFail(
          DatabaseError(
            new Throwable(s"Could Not Find Discussion with ID: $id")
          )
        )

    } yield prepareOutgoingDiscussionRow(discussion))
      .mapError(DatabaseError)
      .provideEnvironment(ZEnvironment(dataSource))

  }

  override def getGeneralDiscussionId(
      spaceId: UUID
  ): ZIO[Any, AppError, GeneralDiscussionId] = {
    for {
      discussionQuery <- run(
        query[Discussions].filter(_.spaceId == lift(spaceId))
      ).mapError(DatabaseError(_))
        .provideEnvironment(ZEnvironment(dataSource))
      dis <- ZIO
        .fromOption(discussionQuery.headOption)
        .orElseFail(DatabaseError(new Throwable("Can't Find Discussion")))
        .provideEnvironment(ZEnvironment(dataSource))
    } yield GeneralDiscussionId(dis.id)
  }

  override def getUserDiscussions(
      requestingUserId: String,
      userId: String,
      skip: Int
  ): ZIO[Any, AppError, List[OutgoingDiscussion]] = {
    (for {

      discussionsUsersLinksJoin <- run(
        getAllUserDiscussions(lift(requestingUserId), lift(skip), lift(userId))
      )
      discussions = discussionsUsersLinksJoin.map { row =>
        prepareOutgoingDiscussionRow(row)
      }
    } yield discussions)
      .mapError(DatabaseError(_))
      .provideEnvironment(ZEnvironment(dataSource))

  }

  override def getSimilarDiscussions(
      discussionId: UUID
  ): ZIO[Any, AppError, List[OutgoingDiscussion]] = {
    (for {
      discussionsUsersLinksJoin <- run(
        getSimilarDiscussionsQuery(lift(discussionId))
      )
      discussions <- ZIO
        .foreachPar(discussionsUsersLinksJoin)(row =>
          ZIO.attempt(
            prepareOutgoingDiscussionRow(
              row
                .into[DiscussionsData]
                .withFieldConst(_.userLikeState, NeutralState.some)
                .withFieldConst(_.userFollowState, false)
                .transform
            )
          )
        )
        .withParallelism(10)
    } yield discussions)
      .mapError(DatabaseError)
      .provideEnvironment(ZEnvironment(dataSource))
  }

  override def getPopularDiscussions(
      requestingUserId: String,
      skip: Int
  ): ZIO[Any, AppError, List[OutgoingDiscussion]] = {

    (for {
      discussionsUsersLinksJoin <- run(
        getAllPopularDiscussions(lift(requestingUserId), lift(skip))
      )

      discussions <- ZIO
        .foreachPar(discussionsUsersLinksJoin)(row =>
          ZIO.attempt(
            prepareOutgoingDiscussionRow(
              row
            )
          )
        )
        .withParallelism(10)

    } yield discussions)
      .mapError(DatabaseError)
      .provideEnvironment(ZEnvironment(dataSource))
  }

  override def getFollowedDiscussions(
      requestingUserId: String,
      skip: Int
  ): ZIO[Any, AppError, List[OutgoingDiscussion]] = {
    (for {
      join <- run(
        getAllFollowedDiscussions(lift(requestingUserId), lift(skip))
      )
        .mapError(DatabaseError(_))
        .provideEnvironment(ZEnvironment(dataSource))

      outgoingDiscussions <- ZIO
        .foreachPar(join)(row => {
          ZIO
            .attempt(
              prepareOutgoingDiscussionRow(
                row
              )
            )
            .mapError(DatabaseError)
        })
        .withParallelism(10)

    } yield outgoingDiscussions)
      .mapError(DatabaseError(_))
      .provideEnvironment(ZEnvironment(dataSource))

  }

  private def prepareOutgoingDiscussionRow(
      d: DiscussionsData
  ): OutgoingDiscussion = {
    sanitizeDiscussion(
      d.into[OutgoingDiscussion]
        .withFieldConst(_.likeState, d.userLikeState.getOrElse(NeutralState))
        .withFieldConst(_.spaceTitle, d.spaceTitle.some)
        .withFieldConst(
          _.spaceCategory,
          Some(SpaceCategories.withName(d.spaceCategory))
        )
        .withFieldConst(_.createdByIconSrc, d.iconSrc.getOrElse(""))
        .withFieldConst(_.createdByTag, d.tag)
        .withFieldConst(_.isFollowing, d.userFollowState)
        .withFieldConst(_.commentCount, d.commentCount)
        .withFieldComputed(
          _.externalContentData,
          row =>
            row.linkType.map(lt =>
              ExternalContentData(
                linkType = lt,
                embedId = d.embedId,
                externalContentUrl = d.externalContentUrl.getOrElse(""),
                thumbImgUrl = d.thumbImgUrl
              )
            )
        )
        .transform
    )
  }

  private def sanitizeDiscussion(
      discussion: OutgoingDiscussion
  ): OutgoingDiscussion = {
    if (discussion.reportStatus == ReportStatus.REMOVED.entryName) {
      discussion.copy(
        title = "",
        editorState = None,
        evidenceLinks = None,
        userUploadedImageUrl = None,
        userUploadedVodUrl = None,
        externalContentData = None
      )
    } else {
      discussion
    }
  }

}

object DiscussionRepositoryLive {
  val layer: URLayer[DataSource, DiscussionRepository] =
    ZLayer.fromFunction(DiscussionRepositoryLive.apply _)
}
