package civil.repositories.topics

import civil.models.{
  Comments,
  Discussions,
  ErrorInfo,
  ExternalLinksDiscussions,
  InternalServerError,
  OutgoingDiscussion,
  Users,
  _
}
import civil.repositories.QuillContextHelper
import zio._
import io.scalaland.chimney.dsl._

import java.util.UUID

case class DiscussionWithLinkData(
    discussion: Discussions,
    externalLinks: Option[ExternalLinksDiscussions]
)

trait DiscussionRepository {
  def insertDiscussion(
      discussion: Discussions,
      linkData: Option[ExternalLinksDiscussions]
  ): ZIO[Any, ErrorInfo, Discussions]
  def getDiscussions(
      topicId: UUID
  ): ZIO[Any, ErrorInfo, List[OutgoingDiscussion]]
  def getDiscussion(id: UUID): ZIO[Any, ErrorInfo, OutgoingDiscussion]

  def getGeneralDiscussionId(
      topicId: UUID
  ): ZIO[Any, ErrorInfo, GeneralDiscussionId]

  def getUserDiscussions(
      requestingUserId: String,
      userId: String
  ): ZIO[Any, ErrorInfo, List[OutgoingDiscussion]]
}

object DiscussionRepository {
  def insertDiscussion(
      discussion: Discussions,
      linkData: Option[ExternalLinksDiscussions]
  ): ZIO[Has[DiscussionRepository], ErrorInfo, Discussions] =
    ZIO.serviceWith[DiscussionRepository](
      _.insertDiscussion(discussion, linkData)
    )

  def getDiscussions(
      topicId: UUID
  ): ZIO[Has[DiscussionRepository], ErrorInfo, List[OutgoingDiscussion]] =
    ZIO.serviceWith[DiscussionRepository](_.getDiscussions(topicId))

  def getDiscussion(
      id: UUID
  ): ZIO[Has[DiscussionRepository], ErrorInfo, OutgoingDiscussion] =
    ZIO.serviceWith[DiscussionRepository](_.getDiscussion(id))

  def getGeneralDiscussionId(
      topicId: UUID
  ): ZIO[Has[DiscussionRepository], ErrorInfo, GeneralDiscussionId] =
    ZIO.serviceWith[DiscussionRepository](_.getGeneralDiscussionId(topicId))

  def getUserTopics(
      requestingUserId: String,
      userId: String
  ): ZIO[Has[DiscussionRepository], ErrorInfo, List[OutgoingDiscussion]] =
    ZIO.serviceWith[DiscussionRepository](
      _.getUserDiscussions(requestingUserId, userId)
    )
}

case class DiscussionRepositoryLive() extends DiscussionRepository {
  import QuillContextHelper.ctx._

  override def insertDiscussion(
      discussion: Discussions,
      externalLinks: Option[ExternalLinksDiscussions]
  ): ZIO[Any, ErrorInfo, Discussions] = {

    for {
      discussionWithLinkData <-
        if (externalLinks.isEmpty)
          ZIO
            .effect(transaction {
              val inserted = run(
                query[Discussions]
                  .insert(lift(discussion))
                  .returning(inserted => inserted)
              )
              DiscussionWithLinkData(
                inserted,
                None
              )
            })
            .mapError(e => InternalServerError(e.toString))
        else
          ZIO
            .effect(transaction {
              val inserted = run(
                query[Discussions]
                  .insert(lift(discussion))
                  .returning(inserted => inserted)
              )
              val linkData = run(
                query[ExternalLinksDiscussions]
                  .insert(
                    lift(externalLinks.get.copy(discussionId = inserted.id))
                  )
                  .returning(inserted => inserted)
              )
              DiscussionWithLinkData(
                inserted,
                Some(linkData)
              )
            })
            .mapError(e => InternalServerError(e.toString))
    } yield discussionWithLinkData.discussion
  }

  override def getDiscussions(
      topicId: UUID
  ): ZIO[Any, ErrorInfo, List[OutgoingDiscussion]] = {

    for {
      discussionsUsersLinksJoin <- ZIO
        .effect(
          run(
            query[Discussions]
              .filter(d => d.topicId == lift(topicId))
              .join(query[Users])
              .on(_.createdByUserId == _.userId)
              .leftJoin(query[ExternalLinksDiscussions])
              .on { case ((d, _), l) => d.id == l.discussionId }
              .map { case ((d, u), l) => (d, u, l) }
          )
        )
        .mapError(e => InternalServerError(e.toString))

      discussions <- ZIO
        .effect(discussionsUsersLinksJoin.map { case (d, u, linkData) =>
          val createdByIconSrc = u.iconSrc
          val commentNumbers = run(
            query[Comments]
              .filter(c => c.discussionId == lift(d.id) && c.parentId.isEmpty)
              .groupBy(c => c.sentiment)
              .map { case (sentiment, comments) =>
                (sentiment, comments.size)
              }
          ).toMap

          val totalCommentsAndReplies =
            run(query[Comments].filter(c => c.discussionId == lift(d.id)).size)
          val positiveComments = commentNumbers.getOrElse("POSITIVE", 0L)
          val neutralComments = commentNumbers.getOrElse("NEUTRAL", 0L)
          val negativeComments = commentNumbers.getOrElse("NEGATIVE", 0L)
          d.into[OutgoingDiscussion]
            .withFieldConst(_.liked, false)
            .withFieldConst(_.createdByIconSrc, createdByIconSrc.getOrElse(""))
            .withFieldConst(_.positiveComments, positiveComments)
            .withFieldConst(_.neutralComments, neutralComments)
            .withFieldConst(_.negativeComments, negativeComments)
            .withFieldConst(
              _.allComments,
              negativeComments + neutralComments + positiveComments
            )
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
    } yield discussions
  }

  override def getDiscussion(
      id: UUID
  ): ZIO[Any, ErrorInfo, OutgoingDiscussion] = {

    for {
      discussionsUsersLinksJoin <- ZIO
        .effect(
          run(
            query[Discussions]
              .filter(d => d.id == lift(id))
              .join(query[Users])
              .on(_.createdByUserId == _.userId)
              .leftJoin(query[ExternalLinksDiscussions])
              .on { case ((d, _), l) => d.id == l.discussionId }
              .map { case ((d, u), l) => (d, u, l) }
          )
        )
        .mapError(e => InternalServerError(e.toString))

      discussionUserLinks <- ZIO
        .fromOption(discussionsUsersLinksJoin.headOption)
        .orElseFail(InternalServerError("Can't Find Discussion"))
      iconSrc = discussionUserLinks._2
      discussion = discussionUserLinks._1
      linkData = discussionUserLinks._3
      commentNumbers <- ZIO
        .effect(
          run(
            query[Comments]
              .filter(c => c.discussionId == lift(id) && c.parentId.isEmpty)
              .groupBy(c => c.sentiment)
              .map { case (sentiment, comments) =>
                (sentiment, comments.size)
              }
          ).toMap
        )
        .mapError(e => InternalServerError(e.toString))
      numCommentsAndReplies <- ZIO
        .effect(
          run(query[Comments].filter(c => c.discussionId == lift(id))).size
        )
        .mapError(e => InternalServerError(e.toString))
      positiveComments = commentNumbers.getOrElse("POSITIVE", 0L)
      neutralComments = commentNumbers.getOrElse("NEUTRAL", 0L)
      negativeComments = commentNumbers.getOrElse("NEGATIVE", 0L)
    } yield discussion
      .into[OutgoingDiscussion]
      .withFieldConst(_.liked, false)
      .withFieldConst(_.createdByIconSrc, iconSrc.iconSrc.getOrElse(""))
      .withFieldConst(_.positiveComments, positiveComments)
      .withFieldConst(_.neutralComments, neutralComments)
      .withFieldConst(_.negativeComments, negativeComments)
      .withFieldConst(
        _.allComments,
        negativeComments + neutralComments + positiveComments
      )
      .withFieldConst(_.totalCommentsAndReplies, numCommentsAndReplies.toLong)
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
      topicId: UUID
  ): ZIO[Any, ErrorInfo, GeneralDiscussionId] = {
    for {
      discussion <- ZIO
        .fromOption(
          run(query[Discussions].filter(_.topicId == lift(topicId))).headOption
        )
        .orElseFail(BadRequest("Cannot Find Discussion"))
    } yield GeneralDiscussionId(discussion.id)
  }

  override def getUserDiscussions(
      requestingUserId: String,
      userId: String
  ): ZIO[Any, ErrorInfo, List[OutgoingDiscussion]] = {

    for {
      discussionsUsersLinksJoin <- ZIO
        .effect(
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
          )
        )
        .mapError(e => InternalServerError(e.toString))
      discussions <- ZIO
        .effect(discussionsUsersLinksJoin.map { case (d, u, linkData) =>
          val createdByIconSrc = u.iconSrc
          d.into[OutgoingDiscussion]
            .withFieldConst(_.liked, false)
            .withFieldConst(_.createdByIconSrc, createdByIconSrc.getOrElse(""))
            .withFieldConst(_.positiveComments, 0L)
            .withFieldConst(_.neutralComments, 0L)
            .withFieldConst(_.negativeComments, 0L)
            .withFieldConst(_.allComments, 0L)
            .withFieldConst(_.totalCommentsAndReplies, 0L)
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
    } yield discussions

  }
}

object DiscussionsRepositoryLive {
  val live: ZLayer[Any, Nothing, Has[DiscussionRepository]] =
    ZLayer.succeed(DiscussionRepositoryLive())
}
