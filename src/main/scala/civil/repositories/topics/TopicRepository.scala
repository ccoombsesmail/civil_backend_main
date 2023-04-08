package civil.repositories.topics

import civil.errors.AppError
import civil.errors.AppError.InternalServerError
import civil.models.{Discussions, OutgoingTopic, Recommendations, TopicLikes, TopicVods, Topics, Users, _}
import civil.directives.OutgoingHttp
import civil.repositories.recommendations.RecommendationsRepository
import io.scalaland.chimney.dsl._
import zio.{ZIO, _}
import io.getquill._

import java.util.UUID
import javax.sql.DataSource
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}


case class TopicWithLinkData(
    topic: Topics,
    externalLinks: Option[ExternalLinks]
)
case class TopicRepoHelpers(
    recommendationsRepository: RecommendationsRepository,
    dataSource: DataSource
) {

  import civil.repositories.QuillContext._
  implicit val ec: ExecutionContext = ExecutionContext.global

  private def topicsUsersVodsLinksJoin(fromUserId: Option[String]): Quoted[Query[(((Topics, Users), Option[TopicVods]), Option[ExternalLinks])]] = {
    fromUserId match {
      case Some(userId) =>
        quote {
          query[Topics]
            .filter(_.createdByUserId == lift(userId))
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

  def runTopicMlPipeline(url: String, insertedTopic: Topics): ZIO[Any, Throwable, Unit] = {

    for {
      words <- ZIO.fromFuture { _ => OutgoingHttp.getTopicWordsFromMLService("get-topic-words", url) }
      targetTopicKeyWords <- run(
        query[Topics]
          .filter(t => t.id == lift(insertedTopic.id))
          .update(
            _.topicWords -> lift(words.topicWords)
          )
          .returning(t => (t.id, t.topicWords))
      ).mapError(e => InternalServerError(e.toString)).provideEnvironment(ZEnvironment(dataSource))
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
  }

  private def getSimilarTopics(
      targetTopicData: (UUID, Seq[String]),
      contentUrl: String
  ) = {
    val targetTopicKeyWords = targetTopicData._2.toSet

    for {
      allTopicData  <- run(
          query[Topics]
          .filter(t => t.id != lift(targetTopicData._1))
          .join(query[ExternalLinks])
          .on(_.id == _.topicId)
          .map {case (t, el) => (t.id, t.topicWords, el.externalContentUrl)}
      ).provideEnvironment(ZEnvironment(dataSource))
      potentialRecommendationIds = allTopicData
        .filter({ case (id, words, externalContentUrl) =>
          val wordsAsSet = words.toSet
          val numWordsIntersecting =
            targetTopicKeyWords.intersect(wordsAsSet).size
          numWordsIntersecting > 2
        })
        .map(d => (d._1.toString, d._3))
        .toMap
      recs <- ZIO.fromFuture { _ =>
        OutgoingHttp.getSimilarityScoresBatch(
          "tfidf-batch",
          contentUrl,
          targetTopicData._1,
          potentialRecommendationIds
        )}
    } yield recs

  }

  def getTopicsWithLikeStatus(
      requestingUserID: String,
      fromUserId: Option[String] = None,
      skip: Int = 0
  ): ZIO[Any, InternalServerError, List[OutgoingTopic]] =
    for {
      likes <- run(query[TopicLikes].filter(_.userId == lift(requestingUserID)))
        .mapError(e => InternalServerError(e.toString)).provideEnvironment(ZEnvironment(dataSource))
      likesMap = likes.foldLeft(Map[UUID, Int]()) { (m, t) =>
        m + (t.topicId -> t.value)
      }
      topicsUsersVodsJoin <- run(topicsUsersVodsLinksJoin(fromUserId).drop(lift(skip)).take(5).sortBy(_._1._1._1.createdAt)(Ord.desc))
        .mapError(e => InternalServerError(e.toString))
        .provideEnvironment(ZEnvironment(dataSource))

        tuvj = topicsUsersVodsJoin.map {
          case (((t, u), v), l) =>
            (t, u, v, l)
        }
      outgoingTopics <- ZIO.foreachPar(tuvj)(row => {
        val (topic, user, vod, linkData) = row

        ZIO
          .attempt(
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
      }).withParallelism(10)
    } yield outgoingTopics
}

trait TopicRepository {
  def insertTopic(
      topic: Topics,
      externalContentData: Option[ExternalLinks]
  ): ZIO[Any, AppError, OutgoingTopic]
  def getTopics: ZIO[Any, AppError, List[OutgoingTopic]]
  def getTopicsAuthenticated(
      requestingUserID: String,
      userData: JwtUserClaimsData,
      skip: Int
  ): ZIO[Any, AppError, List[OutgoingTopic]]

  def getTopic(
      id: UUID,
      requestingUserID: String
  ): ZIO[Any, AppError, OutgoingTopic]

  def getUserTopics(
      requestingUserId: String,
      userId: String
  ): ZIO[Any, AppError, List[OutgoingTopic]]
}

object TopicRepository {
  def insertTopic(
      topic: Topics,
      externalContentData: Option[ExternalLinks]
  ): ZIO[TopicRepository, AppError, OutgoingTopic] =
    ZIO.serviceWithZIO[TopicRepository](_.insertTopic(topic, externalContentData))

  def getTopics: ZIO[TopicRepository, AppError, List[OutgoingTopic]] =
    ZIO.serviceWithZIO[TopicRepository](_.getTopics)

  def getTopicsAuthenticated(
      requestingUserID: String,
      userData: JwtUserClaimsData,
      skip: Int
  ): ZIO[TopicRepository, AppError, List[OutgoingTopic]] =
    ZIO.serviceWithZIO[TopicRepository](
      _.getTopicsAuthenticated(requestingUserID, userData, skip)
    )

  def getTopic(
      id: UUID,
      requestingUserID: String
  ): ZIO[TopicRepository, AppError, OutgoingTopic] =
    ZIO.serviceWithZIO[TopicRepository](_.getTopic(id, requestingUserID))

  def getUserTopics(
      requestingUserId: String,
      userId: String
  ): ZIO[TopicRepository, AppError, List[OutgoingTopic]] =
    ZIO.serviceWithZIO[TopicRepository](_.getUserTopics(requestingUserId, userId))
}

case class TopicRepositoryLive(
    recommendationsRepository: RecommendationsRepository,
    dataSource: DataSource
) extends TopicRepository {

  private val helpers = TopicRepoHelpers(recommendationsRepository, dataSource)
  import helpers._
  import civil.repositories.QuillContext._

  override def insertTopic(
      incomingTopic: Topics,
      externalLinks: Option[ExternalLinks]
  ): ZIO[Any, AppError, OutgoingTopic] = {
    for {
      userQuery <-
          run(
            query[Users].filter(u => u.userId == lift(incomingTopic.createdByUserId))
          )
        .mapError(_ =>
          InternalServerError("There Was A Problem Identifying The User")
        ).provideEnvironment(ZEnvironment(dataSource))
      user <- ZIO.fromOption(userQuery.headOption).mapError(e => InternalServerError(e.toString)).provideEnvironment(ZEnvironment(dataSource))
      _ <- ZIO.when(externalLinks.isEmpty)(transaction {
              val inserted = run(
                query[Topics]
                  .insertValue(lift(incomingTopic))
                  .returning(inserted => inserted)
              )
              inserted.map(t => {
                run(
                  query[Discussions].insertValue(lift(getDefaultDiscussion(t)))
                )
              })
            })
            .mapError(e => InternalServerError(e.toString)).provideEnvironment(ZEnvironment(dataSource))
      _ <- ZIO.when(externalLinks.isDefined)(transaction {
              val inserted = run(
                query[Topics]
                  .insertValue(lift(incomingTopic))
                  .returning(inserted => inserted)
              )
              inserted.map(t => {
                run(
                  query[Discussions].insertValue(lift(getDefaultDiscussion(t)))
                )
                run(
                  query[ExternalLinks]
                    .insertValue(lift(externalLinks.get.copy(topicId = t.id)))
                )
              })
            })
            .mapError(e => InternalServerError(e.toString)).provideEnvironment(ZEnvironment(dataSource))

      topicWithLinkData = TopicWithLinkData(incomingTopic, externalLinks)

      topic: Topics = topicWithLinkData.topic
      linkData = topicWithLinkData.externalLinks
      _ = topic.userUploadedVodUrl.map(url =>
        run(
          query[TopicVods].insertValue(
            lift(TopicVods(topic.createdByUserId, url, topic.id))
          )
        ).mapError(e => InternalServerError(e.toString)).provideEnvironment(ZEnvironment(dataSource))
      )
      _ = topicWithLinkData.externalLinks.map(el =>
        runTopicMlPipeline(el.externalContentUrl, topic).mapError(e => InternalServerError(e.toString)).provideEnvironment(ZEnvironment(dataSource))
      )

      outgoingTopic =
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
    } yield outgoingTopic

  }

  override def getTopics: ZIO[Any, AppError, List[OutgoingTopic]] = {

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
      likes <- run(query[TopicLikes])
        .mapError(e => InternalServerError(e.toString)).provideEnvironment(ZEnvironment(dataSource))
      likesMap = likes.foldLeft(Map[UUID, Int]()) { (m, t) =>
        m + (t.topicId -> t.value)
      }
      joinedVals <- run(joined).mapError(e => InternalServerError(e.toString)).provideEnvironment(ZEnvironment(dataSource))
      topicsUsersVodsLinksJoin = joinedVals.map { case (((t, u), v), l) => (t, u, v, l) }

      outgoingTopics <- ZIO.foreach(topicsUsersVodsLinksJoin)(row => {
        val (topic, user, vod, linkData) = row
        ZIO
          .attempt(
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
  ): ZIO[Any, AppError, OutgoingTopic] = {

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
      likes <- run(
            query[TopicLikes].filter(l =>
              l.userId == lift(requestingUserID) && l.topicId == lift(id)
            )
        )
        .mapError(e => InternalServerError(e.toString))
        .provideEnvironment(ZEnvironment(dataSource))
      likesMap = likes.foldLeft(Map[UUID, Int]()) { (m, t) =>
        m + (t.topicId -> t.value)
      }
      joinedVals <- run(joined).mapError(e => InternalServerError(e.toString)).provideEnvironment(ZEnvironment(dataSource))
      topicsUsersVodsLinksJoin = joinedVals.map { case (((t, u), v), l) => (t, u, v, l) }
      _ <- ZIO
        .fail(
          InternalServerError("Can't find topic details")
        )
        .unless(topicsUsersVodsLinksJoin.nonEmpty)
    } yield topicsUsersVodsLinksJoin
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
  ): ZIO[Any, AppError, List[OutgoingTopic]] = {

    getTopicsWithLikeStatus(requestingUserID, None, skip)
  }

  override def getUserTopics(
      requestingUserId: String,
      userId: String
  ): ZIO[Any, AppError, List[OutgoingTopic]] = {
    getTopicsWithLikeStatus(requestingUserId, Some(userId), 0)

  }

}

object TopicRepositoryLive {

  val layer: URLayer[
    DataSource with RecommendationsRepository,
    TopicRepository,
  ] = ZLayer.fromFunction(TopicRepositoryLive.apply _)
}
