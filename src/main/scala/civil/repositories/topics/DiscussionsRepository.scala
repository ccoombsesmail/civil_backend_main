package civil.repositories.topics

import civil.models.{Comments, Discussions, ExternalLinksDiscussions, OutgoingDiscussion, Users, _}
import civil.errors.AppError
import civil.errors.AppError.InternalServerError
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
  ): ZIO[Any, AppError, Discussions]
  def getDiscussions(
      topicId: UUID,
      skip: Int
  ): ZIO[Any, AppError, List[OutgoingDiscussion]]
  def getDiscussion(id: UUID): ZIO[Any, AppError, OutgoingDiscussion]

  def getGeneralDiscussionId(
      topicId: UUID
  ): ZIO[Any, AppError, GeneralDiscussionId]

  def getUserDiscussions(
      requestingUserId: String,
      userId: String
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
      topicId: UUID,
      skip: Int
  ): ZIO[DiscussionRepository, AppError, List[OutgoingDiscussion]] =
    ZIO.serviceWithZIO[DiscussionRepository](_.getDiscussions(topicId, skip))

  def getDiscussion(
      id: UUID
  ): ZIO[DiscussionRepository, AppError, OutgoingDiscussion] =
    ZIO.serviceWithZIO[DiscussionRepository](_.getDiscussion(id))

  def getGeneralDiscussionId(
      topicId: UUID
  ): ZIO[DiscussionRepository, AppError, GeneralDiscussionId] =
    ZIO.serviceWithZIO[DiscussionRepository](_.getGeneralDiscussionId(topicId))

  def getUserTopics(
      requestingUserId: String,
      userId: String
  ): ZIO[DiscussionRepository, AppError, List[OutgoingDiscussion]] =
    ZIO.serviceWithZIO[DiscussionRepository](
      _.getUserDiscussions(requestingUserId, userId)
    )
}

case class DiscussionRepositoryLive() extends DiscussionRepository {
  import QuillContextHelper.ctx._

  override def insertDiscussion(
      discussion: Discussions,
      externalLinks: Option[ExternalLinksDiscussions]
  ): ZIO[Any, AppError, Discussions] = {

    for {
      discussionWithLinkData <-
        if (externalLinks.isEmpty)
          ZIO
            .attempt(transaction {
              val inserted = run(
                query[Discussions]
                  .insertValue(lift(discussion))
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
            .attempt(transaction {
              val inserted = run(
                query[Discussions]
                  .insertValue(lift(discussion))
                  .returning(inserted => inserted)
              )
              val linkData = run(
                query[ExternalLinksDiscussions]
                  .insertValue(
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
      topicId: UUID,
      skip: Int

                             ): ZIO[Any, AppError, List[OutgoingDiscussion]] = {

    for {
      discussionsUsersLinksJoin <- ZIO
        .attempt(
          run(
            query[Discussions]
              .filter(d => d.topicId == lift(topicId))
              .join(query[Users])
              .on(_.createdByUserId == _.userId)
              .leftJoin(query[ExternalLinksDiscussions])
              .on { case ((d, _), l) => d.id == l.discussionId }
              .map { case ((d, u), l) => (d, u, l) }
              .drop(lift(skip)).take(10)          )
        )
        .mapError(e => InternalServerError(e.toString))

      discussions <- ZIO
        .attempt(discussionsUsersLinksJoin.map { case (d, u, linkData) =>
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
  ): ZIO[Any, AppError, OutgoingDiscussion] = {

    for {
      discussionsUsersLinksJoin <- ZIO
        .attempt(
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
        .attempt(
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
        .attempt(
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
  ): ZIO[Any, AppError, GeneralDiscussionId] = {
    for {
      discussion <- ZIO
        .fromOption(
          run(query[Discussions].filter(_.topicId == lift(topicId))).headOption
        )
        .orElseFail(InternalServerError("Cannot Find Discussion"))
    } yield GeneralDiscussionId(discussion.id)
  }

  override def getUserDiscussions(
      requestingUserId: String,
      userId: String
  ): ZIO[Any, AppError, List[OutgoingDiscussion]] = {

    for {
      discussionsUsersLinksJoin <- ZIO
        .attempt(
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
        .attempt(discussionsUsersLinksJoin.map { case (d, u, linkData) =>
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

object DiscussionRepositoryLive {
  val layer: URLayer[Any, DiscussionRepository] = ZLayer.fromFunction(DiscussionRepositoryLive.apply _)
}
