package civil.repositories.discussions

import civil.controllers.Skip
import civil.errors.AppError
import civil.errors.AppError.InternalServerError
import civil.models.NotifcationEvents.DiscussionMLEvent
import civil.models._
import civil.models.actions.{DislikedState, LikeAction, LikedState, NeutralState}
import civil.models.enums.SpaceCategories
import civil.services.KafkaProducerServiceLive
import io.getquill.Ord
import io.scalaland.chimney.dsl._
import zio._

import java.util.UUID
import javax.sql.DataSource

case class DiscussionWithLinkData(
    discussion: Discussions,
    externalLinks: Option[ExternalLinksDiscussions]
)

trait DiscussionRepository {
  def insertDiscussion(
      discussion: Discussions,
      linkData: Option[ExternalLinksDiscussions]
  ): ZIO[Any, AppError, Discussions]
  def getDiscussions(
      spaceId: UUID,
      skip: Int,
      userId: String
  ): ZIO[Any, AppError, List[OutgoingDiscussion]]
  def getDiscussion(id: UUID): ZIO[Any, AppError, OutgoingDiscussion]

  def getGeneralDiscussionId(
      spaceId: UUID
  ): ZIO[Any, AppError, GeneralDiscussionId]

  def getUserDiscussions(
      requestingUserId: String,
      userId: String
  ): ZIO[Any, AppError, List[OutgoingDiscussion]]


  def getSimilarDiscussions(
                             discussionId: UUID
                           ): ZIO[Any, AppError, List[OutgoingDiscussion]]

  def getPopularDiscussions(
      userId: String,
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

  def getDiscussions(
      spaceId: UUID,
      skip: Int,
      userId: String
  ): ZIO[DiscussionRepository, AppError, List[OutgoingDiscussion]] =
    ZIO.serviceWithZIO[DiscussionRepository](_.getDiscussions(spaceId, skip, userId))

  def getDiscussion(
      id: UUID
  ): ZIO[DiscussionRepository, AppError, OutgoingDiscussion] =
    ZIO.serviceWithZIO[DiscussionRepository](_.getDiscussion(id))

  def getGeneralDiscussionId(
      spaceId: UUID
  ): ZIO[DiscussionRepository, AppError, GeneralDiscussionId] =
    ZIO.serviceWithZIO[DiscussionRepository](_.getGeneralDiscussionId(spaceId))

  def getUserDiscussions(
      requestingUserId: String,
      userId: String
  ): ZIO[DiscussionRepository, AppError, List[OutgoingDiscussion]] =
    ZIO.serviceWithZIO[DiscussionRepository](
      _.getUserDiscussions(requestingUserId, userId)
    )

  def getSimilarDiscussions(
                             discussionId: UUID
                        ): ZIO[DiscussionRepository, AppError, List[OutgoingDiscussion]] =
    ZIO.serviceWithZIO[DiscussionRepository](
      _.getSimilarDiscussions(discussionId)
    )

  def getPopularDiscussions(
                      userId: String,
                        skip: Int,

  ): ZIO[DiscussionRepository, AppError, List[OutgoingDiscussion]] =
    ZIO.serviceWithZIO[DiscussionRepository](_.getPopularDiscussions(userId, skip))
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
        .mapError(e => InternalServerError(e.toString))
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
        .mapError(e => InternalServerError(e.toString))
        .provideEnvironment(ZEnvironment(dataSource))

      _ <- kafka.publish(
        DiscussionMLEvent(
          eventType = "DiscussionMLEvent",
          discussionId = discussion.id,
          editorTextContent = s"${discussion.title}: ${discussion.editorTextContent}",
          externalLinks
        ),
        discussion.id.toString,
        DiscussionMLEvent.discussionMLEventSerde,
        "ml-pipeline"
      ).mapError(e => InternalServerError(e.toString))
    } yield discussion
  }

  override def getDiscussions(
      spaceId: UUID,
      skip: Int,
      userId: String // this is the userId of the requester
  ): ZIO[Any, AppError, List[OutgoingDiscussion]] = {

    for {
      discussionsUsersLinksJoin <- run(
        query[Discussions]
          .filter(d => d.spaceId == lift(spaceId))
          .join(query[Users])
          .on(_.createdByUserId == _.userId)
          .leftJoin(query[ExternalLinksDiscussions])
          .on { case ((d, _), l) => d.id == l.discussionId }
          .leftJoin(query[DiscussionLikes].filter(l => l.userId == lift(userId)))
          .on{ case (((d, u), l), dl) => dl.discussionId == d.id }
          .map { case (((d, u), l), dl) => (d, u, l, dl.map(_.likeState)) }
          .drop(lift(skip))
          .take(10)
      )
        .mapError(e => InternalServerError(e.toString))
        .provideEnvironment(ZEnvironment(dataSource))
      discussions <- ZIO
        .collectAll(discussionsUsersLinksJoin.map { case (d, u, linkData, discussionLike) =>
          val createdByIconSrc = u.iconSrc
          for {
            commentNumbers <- run(
              query[Comments]
                .filter(c => c.discussionId == lift(d.id) && c.parentId.isEmpty)
                .groupBy(c => c.sentiment)
                .map { case (sentiment, comments) =>
                  (sentiment, comments.size)
                }
            )
            commentNumbersMap = commentNumbers.toMap
            totalCommentsAndReplies <- run(
              query[Comments].filter(c => c.discussionId == lift(d.id)).size
            )
            positiveComments = commentNumbersMap.getOrElse("POSITIVE", 0L)
            neutralComments = commentNumbersMap.getOrElse("NEUTRAL", 0L)
            negativeComments = commentNumbersMap.getOrElse("NEGATIVE", 0L)
          } yield d
            .into[OutgoingDiscussion]
            .withFieldConst(_.likeState, discussionLike.getOrElse(NeutralState))
            .withFieldConst(_.createdByIconSrc, createdByIconSrc.getOrElse(""))
            .withFieldConst(_.spaceTitle, None)
            .withFieldConst(_.spaceCategory, None)
            .withFieldConst(_.positiveComments, positiveComments)
            .withFieldConst(_.neutralComments, neutralComments)
            .withFieldConst(_.negativeComments, negativeComments)
            .withFieldConst(
              _.allComments,
              negativeComments + neutralComments + positiveComments
            )
            .withFieldConst(_.createdByTag, u.tag)
            .withFieldConst(_.totalCommentsAndReplies, totalCommentsAndReplies)
            .withFieldConst(
              _.externalContentData,
              linkData.map(data =>
                ExternalContentData(
                  linkType = data.linkType,
                  embedId = data.embedId,
                  externalContentUrl = data.externalContentUrl,
                  thumbImgUrl = data.thumbImgUrl
                )
              )
            )
            .transform
        })
        .mapError(e => InternalServerError(e.toString))
        .provideEnvironment(ZEnvironment(dataSource))
    } yield discussions

  }

  override def getDiscussion(
      id: UUID
  ): ZIO[Any, AppError, OutgoingDiscussion] = {

    for {
      discussionsUsersLinksJoin <- run(
        query[Discussions]
          .filter(d => d.id == lift(id))
          .join(query[Users])
          .on(_.createdByUserId == _.userId)
          .leftJoin(query[ExternalLinksDiscussions])
          .on { case ((d, _), l) => d.id == l.discussionId }
          .map { case ((d, u), l) => (d, u, l) }
      )
        .mapError(e => InternalServerError(e.toString))
        .provideEnvironment(ZEnvironment(dataSource))

      discussionUserLinks <- ZIO
        .fromOption(discussionsUsersLinksJoin.headOption)
        .orElseFail(InternalServerError("Can't Find Discussion"))
      user = discussionUserLinks._2
      discussion = discussionUserLinks._1
      linkData = discussionUserLinks._3
      commentNumbers <- run(
        query[Comments]
          .filter(c => c.discussionId == lift(id) && c.parentId.isEmpty)
          .groupBy(c => c.sentiment)
          .map { case (sentiment, comments) =>
            (sentiment, comments.size)
          }
      )
        .mapError(e => InternalServerError(e.toString))
        .provideEnvironment(ZEnvironment(dataSource))
      commentNumbersMap = commentNumbers.toMap
      numCommentsAndReplies <- run(
        query[Comments].filter(c => c.discussionId == lift(id))
      )
        .mapError(e => InternalServerError(e.toString))
        .provideEnvironment(ZEnvironment(dataSource))
      positiveComments = commentNumbersMap.getOrElse("POSITIVE", 0L)
      neutralComments = commentNumbersMap.getOrElse("NEUTRAL", 0L)
      negativeComments = commentNumbersMap.getOrElse("NEGATIVE", 0L)
    } yield discussion
      .into[OutgoingDiscussion]
      .withFieldConst(_.likeState, NeutralState)
      .withFieldConst(_.spaceTitle, None)
      .withFieldConst(_.spaceCategory, None)
      .withFieldConst(_.createdByIconSrc, user.iconSrc.getOrElse(""))
      .withFieldConst(_.positiveComments, positiveComments)
      .withFieldConst(_.neutralComments, neutralComments)
      .withFieldConst(_.negativeComments, negativeComments)
      .withFieldConst(
        _.allComments,
        negativeComments + neutralComments + positiveComments
      )
      .withFieldConst(_.createdByTag, user.tag)
      .withFieldConst(
        _.totalCommentsAndReplies,
        numCommentsAndReplies.size.toLong
      )
      .withFieldConst(
        _.externalContentData,
        linkData.map(data =>
          ExternalContentData(
            linkType = data.linkType,
            embedId = data.embedId,
            externalContentUrl = data.externalContentUrl,
            thumbImgUrl = data.thumbImgUrl
          )
        )
      )
      .transform
  }

  override def getGeneralDiscussionId(
      spaceId: UUID
  ): ZIO[Any, AppError, GeneralDiscussionId] = {
    for {
      discussionQuery <- run(
        query[Discussions].filter(_.spaceId == lift(spaceId))
      ).mapError(e => InternalServerError(e.toString))
        .provideEnvironment(ZEnvironment(dataSource))
      dis <- ZIO
        .fromOption(discussionQuery.headOption)
        .mapError(e => InternalServerError(e.toString))
        .provideEnvironment(ZEnvironment(dataSource))
    } yield GeneralDiscussionId(dis.id)
  }

  override def getUserDiscussions(
      requestingUserId: String,
      userId: String
  ): ZIO[Any, AppError, List[OutgoingDiscussion]] = {

    for {
      discussionsUsersLinksJoin <-
        run(
          query[Discussions]
            .filter(d =>
              d.createdByUserId == lift(
                requestingUserId
              ) && d.title != "General"
            )
            .join(query[Users])
            .on(_.createdByUserId == _.userId)
            .leftJoin(query[ExternalLinksDiscussions])
            .on { case ((d, _), l) => d.id == l.discussionId }
            .map { case ((d, u), l) => (d, u, l) }
        ).mapError(e => InternalServerError(e.toString))
          .provideEnvironment(ZEnvironment(dataSource))
      discussions = discussionsUsersLinksJoin.map { case (d, u, linkData) =>
        val createdByIconSrc = u.iconSrc
        d.into[OutgoingDiscussion]
          .withFieldConst(_.likeState, NeutralState)
          .withFieldConst(_.spaceTitle, None)
          .withFieldConst(_.spaceCategory, None)
          .withFieldConst(_.createdByIconSrc, createdByIconSrc.getOrElse(""))
          .withFieldConst(_.positiveComments, 0L)
          .withFieldConst(_.neutralComments, 0L)
          .withFieldConst(_.negativeComments, 0L)
          .withFieldConst(_.allComments, 0L)
          .withFieldConst(_.totalCommentsAndReplies, 0L)
          .withFieldConst(_.createdByTag, u.tag)
          .withFieldConst(
            _.externalContentData,
            linkData.map(data =>
              ExternalContentData(
                linkType = data.linkType,
                embedId = data.embedId,
                externalContentUrl = data.externalContentUrl,
                thumbImgUrl = data.thumbImgUrl
              )
            )
          )
          .transform
      }
    } yield discussions

  }

  override def getSimilarDiscussions(discussionId: UUID): ZIO[Any, AppError, List[OutgoingDiscussion]] = {
    for {
      discussionsWithUser <- run(query[Discussions]
        .join(query[Users])
        .on(_.createdByUserId == _.userId)
        .join(query[DiscussionSimilarityScores])
        .on { case ((d, _), dss) => (dss.discussionId1 == lift(discussionId) && d.id == dss.discussionId2) || (dss.discussionId2 == lift(discussionId) && d.id == dss.discussionId1) }
        .leftJoin(query[ExternalLinksDiscussions])
        .on { case (((d, _), _), l) => d.id == l.discussionId }
        .filter { case (((d, _), _), l) => d.id != lift(discussionId) }
        .map { case (((d, u), dss), l) => (d, u, dss, l)}
        .sortBy(_._3.similarityScore)(Ord.desc)
        .take(30))
        .mapError(e => InternalServerError(e.toString))
        .provideEnvironment(ZEnvironment(dataSource))

      outgoingDiscussions <- ZIO.foreachPar(discussionsWithUser)(row => {
        val (space, user, dss, linkData) = row
        ZIO
          .attempt(space.into[OutgoingDiscussion]
            .withFieldConst(_.createdByIconSrc, user.iconSrc.get)
            .withFieldConst(
              _.likeState,
              NeutralState
            )
            .withFieldConst(_.spaceTitle, None)
            .withFieldConst(_.spaceCategory, None)
            .withFieldConst(_.positiveComments, 0L)
            .withFieldConst(_.neutralComments, 0L)
            .withFieldConst(_.negativeComments, 0L)
            .withFieldConst(_.allComments, 0L)
            .withFieldConst(_.totalCommentsAndReplies, 0L)
            .withFieldConst(_.createdByTag, user.tag)
            .withFieldConst(
              _.externalContentData,
              linkData.map(data =>
                ExternalContentData(
                  linkType = data.linkType,
                  embedId = data.embedId,
                  externalContentUrl = data.externalContentUrl,
                  thumbImgUrl = data.thumbImgUrl
                )
              )
            )
            .transform).mapError(e => InternalServerError(e.toString))
          .provideEnvironment(ZEnvironment(dataSource))
      })
    } yield outgoingDiscussions
  }

  override def getPopularDiscussions(userId: String, skip: Int): ZIO[Any, AppError, List[OutgoingDiscussion]] = {

    for {
      discussionsUsersLinksJoin <- run(
        query[Discussions]
          .join(query[Spaces])
          .on(_.spaceId == _.id)
          .join(query[Users])
          .on { case ((d, s), u) => d.createdByUserId == u.userId }
          .leftJoin(query[ExternalLinksDiscussions])
          .on { case (((d, s), u), l) => d.id == l.discussionId }
          .leftJoin(query[DiscussionLikes].filter(l => l.userId == lift(userId)))
          .on { case ((((d, s), u), l), dl) => dl.discussionId == d.id }
          .map { case  ((((d, s), u), l), dl) => (d, s, u, l, dl.map(_.likeState)) }
          .sortBy(_._1.popularityScore)(Ord.desc)
          .drop(lift(skip))
          .take(10)
      )
        .mapError(e => InternalServerError(e.toString))
        .provideEnvironment(ZEnvironment(dataSource))
      discussions <- ZIO
        .collectAll(discussionsUsersLinksJoin.map { case (d, s, u, linkData, discussionLike) =>
          val createdByIconSrc = u.iconSrc
          for {
            commentNumbers <- run(
              query[Comments]
                .filter(c => c.discussionId == lift(d.id) && c.parentId.isEmpty)
                .groupBy(c => c.sentiment)
                .map { case (sentiment, comments) =>
                  (sentiment, comments.size)
                }
            )
            commentNumbersMap = commentNumbers.toMap
            totalCommentsAndReplies <- run(
              query[Comments].filter(c => c.discussionId == lift(d.id)).size
            )
            positiveComments = commentNumbersMap.getOrElse("POSITIVE", 0L)
            neutralComments = commentNumbersMap.getOrElse("NEUTRAL", 0L)
            negativeComments = commentNumbersMap.getOrElse("NEGATIVE", 0L)
          } yield d
            .into[OutgoingDiscussion]
            .withFieldConst(_.likeState, discussionLike.getOrElse(NeutralState))
            .withFieldConst(_.createdByIconSrc, createdByIconSrc.getOrElse(""))
            .withFieldConst(_.spaceTitle, Some(s.title))
            .withFieldConst(_.spaceCategory, Some(SpaceCategories.withName(s.category)))
            .withFieldConst(_.positiveComments, positiveComments)
            .withFieldConst(_.neutralComments, neutralComments)
            .withFieldConst(_.negativeComments, negativeComments)
            .withFieldConst(
              _.allComments,
              negativeComments + neutralComments + positiveComments
            )
            .withFieldConst(_.createdByTag, u.tag)
            .withFieldConst(_.totalCommentsAndReplies, totalCommentsAndReplies)
            .withFieldConst(
              _.externalContentData,
              linkData.map(data =>
                ExternalContentData(
                  linkType = data.linkType,
                  embedId = data.embedId,
                  externalContentUrl = data.externalContentUrl,
                  thumbImgUrl = data.thumbImgUrl
                )
              )
            )
            .transform
        })
        .mapError(e => InternalServerError(e.toString))
        .provideEnvironment(ZEnvironment(dataSource))
    } yield discussions
  }
}

object DiscussionRepositoryLive {
  val layer: URLayer[DataSource, DiscussionRepository] =
    ZLayer.fromFunction(DiscussionRepositoryLive.apply _)
}
