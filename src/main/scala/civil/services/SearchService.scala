package civil.services

import cats.implicits.catsSyntaxOptionId
import civil.errors.AppError
import civil.errors.AppError.InternalServerError
import civil.models._
import civil.repositories.comments.CommentsRepository
import civil.repositories.discussions.DiscussionRepository
import civil.repositories.spaces.SpacesRepository
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
    spaceRepository: SpacesRepository,
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
      query[Spaces]
        .map(t =>
          (
            t.id,
            t.editorTextContent,
            t.createdByUserId,
            t.spaceId,
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
              Some(d.spaceId),
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
              Some(c.spaceId),
              Some(c.discussionId)
            )
          )
          .join(query[Users])
          .on(_._3 == _.userId)
          .filter { case (c, u) => c._2 like lift(filter) }
          .take(5)
    }
    for {
      res <- run(q)
        .mapError(InternalServerError)
        .provideEnvironment(ZEnvironment(dataSource))
    } yield res.map {
      case ((id, textContent, createdByUserId, spaceId, discussionId), u) =>
        (spaceId, discussionId) match {
          case (None, None) =>
            SearchResult(
              space = Space(id, textContent).some,
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
          case (None, Some(_)) =>
            SearchResult(
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
          .mapError(InternalServerError)
          .provideEnvironment(ZEnvironment(dataSource))
      users = res.map { case (userId, tag, bio, username, iconSrc) =>
        User(userId, iconSrc, tag, username, bio)
      }
    } yield users.map(u => SearchResult(user = u))
  }
}

object SearchServiceLive {
  val layer: URLayer[
    SpacesRepository
      with DiscussionRepository
      with CommentsRepository
      with DataSource,
    SearchService
  ] = ZLayer.fromFunction(SearchServiceLive.apply _)
}
