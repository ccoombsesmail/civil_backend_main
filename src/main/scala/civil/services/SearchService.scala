package civil.services

import cats.implicits.catsSyntaxOptionId
import civil.models.{Comment, Comments, Discussion, Discussions, ErrorInfo, InternalServerError, Report, ReportInfo, Reports, SearchResult, Topic, Topics, User, Users}
import civil.repositories.{QuillContextHelper, ReportsRepository}
import civil.repositories.comments.CommentsRepository
import civil.repositories.topics.{DiscussionRepository, TopicRepository}
import io.getquill.{Literal, MirrorSqlDialect, Query, SqlMirrorContext}
import io.scalaland.chimney.dsl.TransformerOps
import zio.{Has, ZIO, ZLayer}


trait SearchService {
  def searchAll(filterText: String): ZIO[Any, ErrorInfo, List[SearchResult]]
  def searchAllUsers(filterText: String): ZIO[Any, ErrorInfo, List[SearchResult]]

}

object SearchService {
  def searchAll(
      filterText: String
  ): ZIO[Has[SearchService], ErrorInfo, List[SearchResult]] =
    ZIO.serviceWith[SearchService](
      _.searchAll(filterText)
    )

  def searchAllUsers(
                 filterText: String
               ): ZIO[Has[SearchService], ErrorInfo, List[SearchResult]] =
    ZIO.serviceWith[SearchService](
      _.searchAllUsers(filterText)
    )
}

case class SearchServiceLive(
    topicRepository: TopicRepository,
    discussionRepository: DiscussionRepository,
    commentsRepository: CommentsRepository
) extends SearchService {

  import QuillContextHelper.ctx._

  override def searchAll(
                          filterText: String
                        ): ZIO[Any, ErrorInfo, List[SearchResult]] = {

    val filter: String = s"%$filterText%"

      val q = quote {
      query[Topics]
        .map(t => (t.id, t.editorTextContent, t.createdByUserId, t.topicId, t.discussionId))
        .join(query[Users]).on(_._3 == _.userId)
        .filter { case (t, u) => t._2 like lift(filter) }.take(5) ++
      query[Discussions]
        .filter(_.title != "General")
        .map(d => (d.id, d.editorTextContent, d.createdByUserId, Some(d.topicId), d.discussionId))
        .join(query[Users]).on(_._3 == _.userId)
        .filter { case (d, u) => d._2 like lift(filter) }.take(5) ++
      query[Comments]
        .map(c => (c.id, c.editorTextContent, c.createdByUserId, Some(c.topicId), Some(c.discussionId)))
        .join(query[Users]).on(_._3 == _.userId)
        .filter { case (c, u) => c._2 like lift(filter) }.take(5)
    }
    for {
      res <- ZIO.effect(run(q)).mapError(e => InternalServerError(e.getMessage))
    } yield res.map { case ((id, textContent, createdByUserId, topicId, discussionId), u) => (topicId, discussionId) match {
      case (None, None) => SearchResult(topic = Topic(id, textContent).some, user = u.transformInto[User])
      case (Some(tId), None) => SearchResult(discussion = Discussion(id, textContent, tId).some, user = u.transformInto[User])
      case (Some(tId), Some(dId)) => SearchResult(comment = Comment(id, textContent, tId, dId).some, user = u.transformInto[User])
    }}

  }

  override def searchAllUsers(filterText: String): ZIO[Any, ErrorInfo, List[SearchResult]] = {
    val filter: String = s"%$filterText%"

    for {
      res <- ZIO.effect(run(query[Users]
        .filter(u => (u.username like lift(filter)) || (u.tag.getOrNull like lift(filter)))
        .map(u => (u.userId, u.tag, u.bio, u.username, u.iconSrc)))).mapError(e => InternalServerError(e.getMessage))
      users = res.map { case (userId, tag, bio, username, iconSrc ) => User(userId, iconSrc, tag, username, bio)}
    } yield users.map(u => SearchResult(user = u))
  }
}

object SearchServiceLive {
  val live: ZLayer[Has[TopicRepository]
    with Has[DiscussionRepository]
    with Has[CommentsRepository], Nothing, Has[
    SearchService
  ]] = {
    for {
      topicsRepo <- ZIO.service[TopicRepository]
      discussionsRepo <- ZIO.service[DiscussionRepository]
      commentsRepo <- ZIO.service[CommentsRepository]

    } yield SearchServiceLive(topicsRepo, discussionsRepo, commentsRepo)
  }.toLayer
}
