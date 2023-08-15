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
      userId: String
  ): ZIO[Any, AppError, OutgoingDiscussion] = {

    for {
      discussionsUsersLinksJoin <- run(
        query[Discussions]
          .filter(d => d.id == lift(id))
          .join(query[Users])
          .on(_.createdByUserId == _.userId)
          .leftJoin(query[ExternalLinksDiscussions])
          .on { case ((d, _), l) => d.id == l.discussionId }
          .leftJoin(
            query[DiscussionLikes].filter(l => l.userId == lift(userId))
          )
          .on { case (((d, _), l), dl) => d.id == dl.discussionId }
          .map { case (((d, u), l), dl) => (d, u, l, dl.map(_.likeState)) }
      )
        .mapError(DatabaseError(_))
        .provideEnvironment(ZEnvironment(dataSource))
      discussionUserLinksLike <- ZIO
        .fromOption(discussionsUsersLinksJoin.headOption)
        .orElseFail(DatabaseError(new Throwable("Can't Find Discussion")))
      (discussion, user, linkData, likeState) = discussionUserLinksLike
      totalCommentsAndReplies <- run(
        query[Comments].filter(c => c.discussionId == lift(discussion.id)).size
      )
        .mapError(DatabaseError(_))
        .provideEnvironment(ZEnvironment(dataSource))
    } yield prepareOutgoingDiscussion(
      discussion,
      linkData,
      user,
      totalCommentsAndReplies,
      likeState
    )
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
    for {
      discussionsWithUser <- run(
        query[Discussions]
          .join(query[Users])
          .on(_.createdByUserId == _.userId)
          .join(query[DiscussionSimilarityScores])
          .on { case ((d, _), dss) =>
            (dss.discussionId1 == lift(
              discussionId
            ) && d.id == dss.discussionId2) || (dss.discussionId2 == lift(
              discussionId
            ) && d.id == dss.discussionId1)
          }
          .leftJoin(query[ExternalLinksDiscussions])
          .on { case (((d, _), _), l) => d.id == l.discussionId }
          .filter { case (((d, _), _), l) => d.id != lift(discussionId) }
          .map { case (((d, u), dss), l) => (d, u, dss, l) }
          .sortBy(_._3.similarityScore)(Ord.desc)
          .take(30)
      )
        .mapError(DatabaseError(_))
        .provideEnvironment(ZEnvironment(dataSource))

      outgoingDiscussions <- ZIO.foreachPar(discussionsWithUser)(row => {
        val (discussion, user, dss, linkData) = row
        ZIO
          .attempt(
            prepareOutgoingDiscussion(
              discussion,
              linkData,
              user,
              0L,
              None
            )
          )
          .mapError(DatabaseError(_))
          .provideEnvironment(ZEnvironment(dataSource))
      })
    } yield outgoingDiscussions
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

  private def fetchDiscussionUserLinkLike(
      spaceId: UUID,
      userId: String,
      skip: Int
  ): IO[AppError, List[
    (
        Discussions,
        Users,
        Option[ExternalLinksDiscussions],
        Option[LikeAction],
        Option[DiscussionFollows]
    )
  ]] = {
    run(
      query[Discussions]
        .filter(d => d.spaceId == lift(spaceId))
        .join(query[Users])
        .on(_.createdByUserId == _.userId)
        .leftJoin(query[ExternalLinksDiscussions])
        .on { case ((d, _), l) => d.id == l.discussionId }
        .leftJoin(
          query[DiscussionLikes].filter(l => l.userId == lift(userId))
        )
        .on { case (((d, _), l), dl) => d.id == dl.discussionId }
        .leftJoin(
          query[DiscussionFollows].filter(l => l.userId == lift(userId))
        )
        .on { case ((((d, _), l), dl), df) => d.id == df.followedDiscussionId }
        .drop(lift(skip))
        .map { case ((((d, u), l), dl), df) =>
          (d, u, l, dl.map(_.likeState), df)
        }
    )
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

  private def prepareOutgoingDiscussion(
      d: Discussions,
      externalContentData: Option[ExternalLinksDiscussions],
      user: Users,
      commentAndRepliesCount: Long,
      likeState: Option[LikeAction],
      space: Option[Spaces] = None,
      isFollowing: Boolean = false
  ): OutgoingDiscussion = {

    sanitizeDiscussion(
      d.into[OutgoingDiscussion]
        .withFieldConst(_.likeState, likeState.getOrElse(NeutralState))
        .withFieldConst(_.spaceTitle, space.map(_.title))
        .withFieldConst(
          _.spaceCategory,
          space.map(s => SpaceCategories.withName(s.category))
        )
        .withFieldConst(_.createdByIconSrc, user.iconSrc.getOrElse(""))
        .withFieldConst(_.createdByTag, user.tag)
        .withFieldConst(_.isFollowing, isFollowing)
        .withFieldConst(_.commentCount, 0)
        .withFieldConst(
          _.externalContentData,
          externalContentData.map(data =>
            ExternalContentData(
              linkType = data.linkType,
              embedId = data.embedId,
              externalContentUrl = data.externalContentUrl,
              thumbImgUrl = data.thumbImgUrl
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
