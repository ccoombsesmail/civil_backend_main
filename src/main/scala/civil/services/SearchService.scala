package civil.services

import cats.implicits.catsSyntaxOptionId
import civil.errors.AppError
import civil.errors.AppError.GeneralError
import civil.models.{Comment, Comments, Discussion, Discussions, SearchResult, Topic, Topics, User, Users}
import civil.repositories.comments.CommentsRepository
import civil.repositories.topics.{DiscussionRepository, TopicRepository}
import io.scalaland.chimney.dsl.TransformerOps
import zio.{URLayer, ZEnvironment, ZIO, ZLayer}

import javax.sql.DataSource

trait SearchService {
  def searchAll(filterText: String): ZIO[Any, AppError, List[SearchResult]]
  def searchAllUsers(filterText: String): ZIO[Any, AppError, List[SearchResult]]

}

object SearchService {
  def searchAll(
      filterText: String
  ): ZIO[SearchService, AppError, List[SearchResult]] =
    ZIO.serviceWithZIO[SearchService](
      _.searchAll(filterText)
    )

  def searchAllUsers(
      filterText: String
  ): ZIO[SearchService, AppError, List[SearchResult]] =
    ZIO.serviceWithZIO[SearchService](
      _.searchAllUsers(filterText)
    )
}

case class SearchServiceLive(
    topicRepository: TopicRepository,
    discussionRepository: DiscussionRepository,
    commentsRepository: CommentsRepository,
    dataSource: DataSource
) extends SearchService {

  import civil.repositories.QuillContext._

  override def searchAll(
      filterText: String
  ): ZIO[Any, AppError, List[SearchResult]] = {

    val filter: String = s"%$filterText%"

    val q = quote {
      query[Topics]
        .map(t =>
          (
            t.id,
            t.editorTextContent,
            t.createdByUserId,
            t.topicId,
            t.discussionId
          )
        )
        .join(query[Users])
        .on(_._3 == _.userId)
        .filter { case (t, u) => t._2 like lift(filter) }
        .take(5) ++
        query[Discussions]
          .filter(_.title != "General")
          .map(d =>
            (
              d.id,
              d.editorTextContent,
              d.createdByUserId,
              Some(d.topicId),
              d.discussionId
            )
          )
          .join(query[Users])
          .on(_._3 == _.userId)
          .filter { case (d, u) => d._2 like lift(filter) }
          .take(5) ++
        query[Comments]
          .map(c =>
            (
              c.id,
              c.editorTextContent,
              c.createdByUserId,
              Some(c.topicId),
              Some(c.discussionId)
            )
          )
          .join(query[Users])
          .on(_._3 == _.userId)
          .filter { case (c, u) => c._2 like lift(filter) }
          .take(5)
    }
    for {
      res <- run(q).mapError(e => GeneralError(e.getMessage)).provideEnvironment(ZEnvironment(dataSource))
    } yield res.map {
      case ((id, textContent, createdByUserId, topicId, discussionId), u) =>
        (topicId, discussionId) match {
          case (None, None) =>
            SearchResult(
              topic = Topic(id, textContent).some,
              user = u.transformInto[User]
            )
          case (Some(tId), None) =>
            SearchResult(
              discussion = Discussion(id, textContent, tId).some,
              user = u.transformInto[User]
            )
          case (Some(tId), Some(dId)) =>
            SearchResult(
              comment = Comment(id, textContent, tId, dId).some,
              user = u.transformInto[User]
            )
        }
    }

  }

  override def searchAllUsers(
      filterText: String
  ): ZIO[Any, AppError, List[SearchResult]] = {
    val filter: String = s"%$filterText%"

    for {
      res <-
          run(
            query[Users]
              .filter(u =>
                (u.username like lift(filter)) || (u.tag.getOrNull like lift(
                  filter
                ))
              )
              .map(u => (u.userId, u.tag, u.bio, u.username, u.iconSrc))
        )
        .mapError(e => GeneralError(e.getMessage)).provideEnvironment(ZEnvironment(dataSource))
      users = res.map { case (userId, tag, bio, username, iconSrc) =>
        User(userId, iconSrc, tag, username, bio)
      }
    } yield users.map(u => SearchResult(user = u))
  }
}

object SearchServiceLive {
  val layer: URLayer[
    TopicRepository with DiscussionRepository with CommentsRepository with DataSource,
    SearchService
  ] = ZLayer.fromFunction(SearchServiceLive.apply _)
}
