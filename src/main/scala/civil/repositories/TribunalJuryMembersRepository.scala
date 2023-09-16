package civil.repositories

import cats.implicits.catsSyntaxOptionId
import civil.errors.AppError
import civil.errors.AppError.{DatabaseError, InternalServerError}
import civil.models.actions.NeutralState
import civil.models.enums.SpaceCategories
import civil.models.enums.SpaceCategories.General
import civil.models.{
  Comment,
  CommentWithUserData,
  Comments,
  Discussions,
  ExternalContentData,
  ExternalLinksDiscussions,
  JuryDuty,
  OutgoingDiscussion,
  OutgoingSpace,
  Spaces,
  TribunalJuryMembers,
  Users
}
import io.scalaland.chimney.dsl.TransformerOps
import zio.{URLayer, ZEnvironment, ZIO, ZLayer}

import java.util.UUID
import javax.sql.DataSource

trait TribunalJuryMembersRepository {
  def insertJuryMember(
      userId: String,
      contentId: UUID
  ): ZIO[Any, AppError, Unit]

  def getUserJuryDuties(
      userId: String
  ): ZIO[Any, AppError, List[JuryDuty]]

}

object TribunalJuryMembersRepository {
  def insertJuryMember(
      userId: String,
      contentId: UUID
  ): ZIO[TribunalJuryMembersRepository, AppError, Unit] =
    ZIO.serviceWithZIO[TribunalJuryMembersRepository](
      _.insertJuryMember(userId, contentId)
    )

  def getUserJuryDuties(
      userId: String
  ): ZIO[TribunalJuryMembersRepository, AppError, List[JuryDuty]] =
    ZIO.serviceWithZIO[TribunalJuryMembersRepository](
      _.getUserJuryDuties(userId)
    )
}

case class TribunalJuryMembersRepositoryLive(dataSource: DataSource)
    extends TribunalJuryMembersRepository {

  import civil.repositories.QuillContext._

  override def insertJuryMember(
      userId: String,
      contentId: UUID
  ): ZIO[Any, AppError, Unit] = {
    for {
      _ <- run(
        query[TribunalJuryMembers].insertValue(
          lift(
            TribunalJuryMembers(userId, contentId, contentType = "TOPIC", None)
          )
        )
      ).mapError(DatabaseError(_)).provideEnvironment(ZEnvironment(dataSource))
    } yield ()
  }

  override def getUserJuryDuties(
      userId: String
  ): ZIO[Any, AppError, List[JuryDuty]] = (for {
    pastCurrentJuryDuties <- run(
      query[TribunalJuryMembers]
        .filter(_.userId == lift(userId))
        .leftJoin(query[Spaces])
        .on { case (tjm, s) => tjm.contentId == s.id }
        .leftJoin(query[Discussions])
        .on { case ((tjm, s), d) => tjm.contentId == d.id }
        .leftJoin(query[Comments])
        .on { case (((tjm, s), d), c) => tjm.contentId == c.id }
        .join(query[Users])
        .on { case ((((tjm, s), d), c), u) =>
          s.exists(_.createdByUserId == u.userId) || d.exists(
            _.createdByUserId == u.userId
          ) || c.exists(_.createdByUserId == u.userId)
        }
        .leftJoin(query[ExternalLinksDiscussions])
        .on { case (((((tjm, s), d), c), u), edl) =>
          d.exists(_.id == edl.discussionId)
        }
        .map { case (((((tjm, s), d), c), u), edl) => (tjm, s, d, c, u, edl) }
    )
    res = pastCurrentJuryDuties.map {
      case (tjm, s, d, c, u, edl) => {
        val outgoingSpace = s.map(
          _.into[OutgoingSpace]
            .withFieldConst(_.createdByUserId, u.userId)
            .withFieldConst(_.createdByUsername, u.username)
            .withFieldConst(_.createdByIconSrc, u.iconSrc.getOrElse(""))
            .withFieldConst(_.createdByTag, u.tag)
            .withFieldComputed(
              _.category,
              s => SpaceCategories.withName(s.category)
            )
            // Might need to make a different model, or do the necessary query to find these values
            .withFieldConst(_.likeState, NeutralState)
            .withFieldConst(_.isFollowing, false)
            .withFieldConst(_.discussionCount, 0)
            .withFieldConst(_.commentCount, 0)
            .transform
        )
        val outgoingDiscussion = d.map(
          _.into[OutgoingDiscussion]
            .withFieldConst(_.createdByUserId, u.userId)
            .withFieldConst(_.createdByUsername, u.username)
            .withFieldConst(_.createdByIconSrc, u.iconSrc.getOrElse(""))
            .withFieldConst(_.createdByTag, u.tag)
            .withFieldConst(
              _.externalContentData,
              edl.map(_.into[ExternalContentData].transform)
            )
            // Might need to make a different model, or do the necessary query to find these values
            .withFieldConst(_.likeState, NeutralState)
            .withFieldConst(_.isFollowing, false)
            .withFieldConst(_.commentCount, 0)
            .withFieldConst(_.spaceTitle, "".some)
            .withFieldConst(_.spaceCategory, General.some)
            .transform
        )

        val outGoingComment = c.map(
          _.into[CommentWithUserData]
            .withFieldConst(_.createdByUserId, u.userId)
            .withFieldConst(_.createdByUsername, u.username)
            .withFieldConst(_.createdByIconSrc, u.iconSrc.getOrElse(""))
            .withFieldConst(_.createdByTag, u.tag)
            .transform
        )
        JuryDuty(
          contentId = tjm.contentId,
          contentType = tjm.contentType,
          juryDutyCompletionTime = tjm.juryDutyCompletionTime,
          space = outgoingSpace,
          discussion = outgoingDiscussion,
          comment = outGoingComment
        )
      }
    }

  } yield res)
    .mapError(DatabaseError(_))
    .provideEnvironment(ZEnvironment(dataSource))
}

object TribunalJuryMembersRepositoryLive {
  val layer: URLayer[DataSource, TribunalJuryMembersRepository] =
    ZLayer.fromFunction(TribunalJuryMembersRepositoryLive.apply _)
}
