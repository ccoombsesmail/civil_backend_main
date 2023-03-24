package civil.repositories.topics

import civil.models.{
  Discussions,
  ErrorInfo,
  InternalServerError,
  OutgoingTopic,
  Recommendations,
  TopicLikes,
  TopicVods,
  Topics,
  Users,
  _
}
import civil.directives.OutgoingHttp
import civil.repositories.QuillContextHelper
import civil.repositories.recommendations.RecommendationsRepository
import io.scalaland.chimney.dsl._
import zio.{ZIO, _}
import io.circe.syntax._
import io.getquill._
import io.getquill.context.jdbc._
import io.getquill.{Literal, SnakeCase}
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import java.time.Instant
import java.time.format.DateTimeFormatter




case class TopicWithLinkData(
    topic: Topics,
    externalLinks: Option[ExternalLinks]
)
case class TopicRepoHelpers(
    recommendationsRepository: RecommendationsRepository
) {

  import QuillContextHelper.ctx._

  def topicsUsersVodsLinksJoin(fromUserId: Option[String]) = {
    fromUserId match {
      case Some(userId) =>
        quote {
          query[Topics]
            .filter(_.createdByUserId == lift(userId))
            .sortBy(_.createdAt)
            .join(query[Users])
            .on(_.createdByUserId == _.userId)
            .leftJoin(query[TopicVods])
            .on { case ((t, u), v) => t.id == v.topicId }
            .leftJoin(query[ExternalLinks])
            .on { case (((t, u), v), l) => t.id == l.topicId }
        }
      case None =>
        quote {
          query[Topics]
            .sortBy(_.createdAt)
            .join(query[Users])
            .on(_.createdByUserId == _.userId)
            .leftJoin(query[TopicVods])
            .on { case ((t, u), v) => t.id == v.topicId }
            .leftJoin(query[ExternalLinks])
            .on { case (((t, u), v), l) => t.id == l.topicId }

        }
    }

  }

  def getDefaultDiscussion(topic: Topics) = Discussions(
    id = UUID.randomUUID(),
    topicId = topic.id,
    createdByUsername = topic.createdByUsername,
    title = "General",
    createdAt = topic.createdAt,
    createdByUserId = topic.createdByUserId,
    likes = 0,
    evidenceLinks = None,
    editorState = "General Discussion",
    editorTextContent = "General, Discussion",
    userUploadedVodUrl = None,
    userUploadedImageUrl = None
  )

  def runTopicMlPipeline(url: String, insertedTopic: Topics): Future[Unit] =
    for {
      words <- OutgoingHttp.getTopicWordsFromMLService("get-topic-words", url)
      targetTopicKeyWords = run(
        query[Topics]
          .filter(t => t.id == lift(insertedTopic.id))
          .update(
            _.topicWords -> lift(words.topicWords)
          )
          .returning(t => (t.id, t.topicWords))
      )
      recommendedTopics <- getSimilarTopics(targetTopicKeyWords, url)

      recommendations = recommendedTopics.recs
        .map(r => {
          r.into[Recommendations]
            .withFieldConst(
              _.targetContentId,
              UUID.fromString(r.targetContentId)
            )
            .withFieldConst(
              _.recommendedContentId,
              UUID.fromString(r.recommendedContentId)
            )
            .transform
        })
        .toList
      _ = recommendationsRepository.batchInsertRecommendation(recommendations)
    } yield ()

  def getSimilarTopics(
      targetTopicData: (UUID, Seq[String]),
      contentUrl: String
  ) = {
    val targetTopicKeyWords = targetTopicData._2.toSet
    val allTopicData = run(
      query[Topics]
        .filter(t => t.id != lift(targetTopicData._1))
        .join(query[ExternalLinks])
        .on(_.id == _.topicId)
        .map { case (t, el) => (t.id, t.topicWords, el.externalContentUrl) }
    )
    val potentialRecommendationIds = allTopicData
      .filter({ case (id, words, externalContentUrl) =>
        val wordsAsSet = words.toSet
        val numWordsIntersecting =
          targetTopicKeyWords.intersect(wordsAsSet).size
        numWordsIntersecting > 2
      })
      .map(d => (d._1.toString, d._3))
      .toMap
    OutgoingHttp.getSimilarityScoresBatch(
      "tfidf-batch",
      contentUrl,
      targetTopicData._1,
      potentialRecommendationIds
    )
  }

  def getTopicsWithLikeStatus(
      requestingUserID: String,
      fromUserId: Option[String] = None,
      skip: Int = 0
  ): ZIO[Any, InternalServerError, List[OutgoingTopic]] =
    for {
      likes <- ZIO
        .effect(
          run(query[TopicLikes].filter(_.userId == lift(requestingUserID)))
        )
        .mapError(e => InternalServerError(e.toString))
      likesMap = likes.foldLeft(Map[UUID, Int]()) { (m, t) =>
        m + (t.topicId -> t.value)
      }
      topicsUsersVodsJoin <- ZIO
        .effect(run(topicsUsersVodsLinksJoin(fromUserId).drop(lift(skip)).take(5)).map {
          case (((t, u), v), l) =>
            (t, u, v, l)
        })
        .mapError(e => InternalServerError(e.toString))

      outgoingTopics <- ZIO.foreachParN(10)(topicsUsersVodsJoin)(row => {
        val (topic, user, vod, linkData) = row

        ZIO
          .effect(
            topic
              .into[OutgoingTopic]
              .withFieldConst(_.createdByIconSrc, user.iconSrc.get)
              .withFieldConst(_.likeState, likesMap.getOrElse(topic.id, 0))
              .withFieldConst(_.userUploadedVodUrl, vod.map(v => v.vodUrl))
              .withFieldConst(_.topicCreatorIsDidUser, user.isDidUser)
              .withFieldConst(_.createdByTag, user.tag)
              .withFieldComputed(_.editorState, row => row.editorState)
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
          )
          .mapError(e => InternalServerError(e.getMessage))
      })
    } yield outgoingTopics
}

trait TopicRepository {
  def insertTopic(
      topic: Topics,
      externalContentData: Option[ExternalLinks]
  ): ZIO[Any, ErrorInfo, OutgoingTopic]
  def getTopics: ZIO[Any, ErrorInfo, List[OutgoingTopic]]
  def getTopicsAuthenticated(
      requestingUserID: String,
      userData: JwtUserClaimsData,
      skip: Int
  ): ZIO[Any, ErrorInfo, List[OutgoingTopic]]

  def getTopic(
      id: UUID,
      requestingUserID: String
  ): ZIO[Any, ErrorInfo, OutgoingTopic]

  def getUserTopics(
      requestingUserId: String,
      userId: String
  ): ZIO[Any, ErrorInfo, List[OutgoingTopic]]
}

object TopicRepository {
  def insertTopic(
      topic: Topics,
      externalContentData: Option[ExternalLinks]
  ): ZIO[Has[TopicRepository], ErrorInfo, OutgoingTopic] =
    ZIO.serviceWith[TopicRepository](_.insertTopic(topic, externalContentData))

  def getTopics: ZIO[Has[TopicRepository], ErrorInfo, List[OutgoingTopic]] =
    ZIO.serviceWith[TopicRepository](_.getTopics)

  def getTopicsAuthenticated(
      requestingUserID: String,
      userData: JwtUserClaimsData,
      skip: Int
  ): ZIO[Has[TopicRepository], ErrorInfo, List[OutgoingTopic]] =
    ZIO.serviceWith[TopicRepository](
      _.getTopicsAuthenticated(requestingUserID, userData, skip)
    )

  def getTopic(
      id: UUID,
      requestingUserID: String
  ): ZIO[Has[TopicRepository], ErrorInfo, OutgoingTopic] =
    ZIO.serviceWith[TopicRepository](_.getTopic(id, requestingUserID))

  def getUserTopics(
      requestingUserId: String,
      userId: String
  ): ZIO[Has[TopicRepository], ErrorInfo, List[OutgoingTopic]] =
    ZIO.serviceWith[TopicRepository](_.getUserTopics(requestingUserId, userId))
}

case class TopicRepositoryLive(
    recommendationsRepository: RecommendationsRepository
) extends TopicRepository {
  import QuillContextHelper.ctx._

  val helpers = TopicRepoHelpers(recommendationsRepository)
  import helpers._

  override def insertTopic(
      topic: Topics,
      externalLinks: Option[ExternalLinks]
  ): ZIO[Any, ErrorInfo, OutgoingTopic] = {
    for {
      user <- ZIO
        .fromOption(
          run(
            query[Users].filter(u => u.userId == lift(topic.createdByUserId))
          ).headOption
        )
        .mapError(_ =>
          InternalServerError("There Was A Problem Identifying The User")
        )
      topicWithLinkData <-
        if (externalLinks.isEmpty)
          ZIO
            .effect(transaction {
              val inserted = run(
                query[Topics]
                  .insert(lift(topic.copy(editorState = topic.editorState)))
                  .returning(inserted => inserted)
              )
              run(
                query[Discussions].insert(lift(getDefaultDiscussion(inserted)))
              )
              TopicWithLinkData(
                inserted,
                None
              )
            })
            .mapError(e => InternalServerError(e.toString))
        else
          ZIO
            .effect(transaction {
              val inserted = run(
                query[Topics]
                  .insert(lift(topic))
                  .returning(inserted => inserted)
              )
              run(
                query[Discussions].insert(lift(getDefaultDiscussion(inserted)))
              )
              val linkData = run(
                query[ExternalLinks]
                  .insert(lift(externalLinks.get.copy(topicId = inserted.id)))
                  .returning(inserted => inserted)
              )
              TopicWithLinkData(
                inserted,
                Some(linkData)
              )
            })
            .mapError(e => InternalServerError(e.toString))
      topic = topicWithLinkData.topic
      linkData = topicWithLinkData.externalLinks
      _ = topic.userUploadedVodUrl.map(url =>
        run(
          query[TopicVods].insert(
            lift(TopicVods(topic.createdByUserId, url, topic.id))
          )
        )
      )
      _ = topicWithLinkData.externalLinks.map(el =>
        runTopicMlPipeline(el.externalContentUrl, topic)
      )

      outgoingTopic <- ZIO
        .effect(
          topic
            .into[OutgoingTopic]
            .withFieldConst(_.createdByIconSrc, user.iconSrc.getOrElse(""))
            .withFieldConst(_.likeState, 0)
            .withFieldConst(_.createdByTag, user.tag)
            .withFieldConst(_.topicCreatorIsDidUser, user.isDidUser)
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
        )
        .mapError(_ =>
          InternalServerError(
            "There Was A Problem Identifying Saving The Topic"
          )
        )
    } yield outgoingTopic

  }

  override def getTopics: ZIO[Any, ErrorInfo, List[OutgoingTopic]] = {

    val joined = quote {
      query[Topics]
        .join(query[Users])
        .on(_.createdByUserId == _.userId)
        .leftJoin(query[TopicVods])
        .on { case ((t, u), v) => t.id == v.topicId }
        .leftJoin(query[ExternalLinks])
        .on { case (((t, u), v), l) => t.id == l.topicId }
    }

    for {
      likes <- ZIO
        .effect(run(query[TopicLikes]))
        .mapError(e => InternalServerError(e.toString))
      likesMap = likes.foldLeft(Map[UUID, Int]()) { (m, t) =>
        m + (t.topicId -> t.value)
      }
      topicsUsersVodsLinksJoin <- ZIO
        .effect(run(joined).map { case (((t, u), v), l) => (t, u, v, l) })
        .mapError(e => InternalServerError(e.toString))
      outgoingTopics <- ZIO.foreach(topicsUsersVodsLinksJoin)(row => {
        val (topic, user, vod, linkData) = row
        ZIO
          .effect(
            topic
              .into[OutgoingTopic]
              .withFieldConst(_.createdByIconSrc, user.iconSrc.get)
              .withFieldConst(_.likeState, likesMap.getOrElse(topic.id, 0))
              .withFieldConst(_.userUploadedVodUrl, vod.map(v => v.vodUrl))
              .withFieldConst(_.topicCreatorIsDidUser, user.isDidUser)
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
              .transform
          )
          .mapError(e => InternalServerError(e.toString))
      })
    } yield outgoingTopics.sortWith((t1, t2) =>
      t2.createdAt.isBefore(t1.createdAt)
    )

  }

  override def getTopic(
      id: UUID,
      requestingUserID: String
  ): ZIO[Any, ErrorInfo, OutgoingTopic] = {

    val joined = quote {
      query[Topics]
        .filter(t => t.id == lift(id))
        .join(query[Users])
        .on(_.createdByUserId == _.userId)
        .leftJoin(query[TopicVods])
        .on { case ((t, u), v) => t.id == v.topicId }
        .leftJoin(query[ExternalLinks])
        .on { case (((t, u), v), l) => t.id == l.topicId }
    }

    for {
      likes <- ZIO
        .effect(
          run(
            query[TopicLikes].filter(l =>
              l.userId == lift(requestingUserID) && l.topicId == lift(id)
            )
          )
        )
        .tapError(e => {
          println(e)
          ZIO.fail(e)
        })
        .mapError(e => BadRequest(e.toString))
      likesMap = likes.foldLeft(Map[UUID, Int]()) { (m, t) =>
        m + (t.topicId -> t.value)
      }
      topicsUsersVodsLinkJoin <- ZIO
        .effect(run(joined).map { case (((t, u), v), l) => (t, u, v, l) })
        .tapError(e => {
          println(e)
          ZIO.fail(e)
        })
        .mapError(e => BadRequest(e.toString))
      _ <- ZIO
        .fail(
          InternalServerError("Can't find topic details")
        )
        .unless(topicsUsersVodsLinkJoin.nonEmpty)
    } yield topicsUsersVodsLinkJoin
      .map(row => {
        val (topic, user, vod, linkData) = row

        topic
          .into[OutgoingTopic]
          .withFieldConst(_.createdByIconSrc, user.iconSrc.get)
          .withFieldConst(_.likeState, likesMap.getOrElse(topic.id, 0))
          .withFieldConst(_.userUploadedVodUrl, None)
          .withFieldConst(_.topicCreatorIsDidUser, user.isDidUser)
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
          .transform
      })
      .head

  }

  override def getTopicsAuthenticated(
      requestingUserID: String,
      userData: JwtUserClaimsData,
      skip: Int
  ): ZIO[Any, ErrorInfo, List[OutgoingTopic]] = {

    getTopicsWithLikeStatus(requestingUserID, None, skip)
  }

  override def getUserTopics(
      requestingUserId: String,
      userId: String
  ): ZIO[Any, ErrorInfo, List[OutgoingTopic]] = {
    println(requestingUserId, userId)
    getTopicsWithLikeStatus(requestingUserId, Some(userId), 0)

  }

}

object TopicRepositoryLive {
  val live: ZLayer[Has[RecommendationsRepository], Nothing, Has[
    TopicRepository
  ]] = {
    for {
      topicRepo <- ZIO.service[RecommendationsRepository]
    } yield TopicRepositoryLive(topicRepo)
  }.toLayer
}
