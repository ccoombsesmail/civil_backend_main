package civil.repositories

import civil.errors.AppError
import civil.errors.AppError.InternalServerError
import civil.models.actions._
import civil.models.enums.ReportStatus.CLEAN
import civil.models.enums.{LinkType, ReportStatus, UserVerificationType}
import civil.models.enums.UserVerificationType.NO_VERIFICATION
import civil.models.{
  CommentWithDepthAndUser,
  SpaceFollows,
  SpaceLikes,
  Spaces,
  TribunalCommentWithDepth,
  TribunalCommentWithDepthAndUser,
  Users
}
import com.typesafe.config.ConfigFactory
import io.getquill.context.ZioJdbc.DataSourceLayer
import io.getquill.jdbczio.Quill
import io.getquill.{
  EntityQuery,
  PostgresZioJdbcContext,
  Query,
  Quoted,
  SnakeCase
}
import zio.{IO, System, ZEnvironment, ZIO, ZLayer}

import java.sql.SQLException
import java.time.ZonedDateTime
import java.util.UUID
import javax.sql.DataSource
import scala.jdk.CollectionConverters.MapHasAsJava

object QuillContext extends PostgresZioJdbcContext(SnakeCase) with QuillCodecs {

  val dataSourceLayer: ZLayer[Any, Nothing, DataSource] =
    ZLayer {
      for {
        herokuURL <- System.env("DATABASE_URL").orDie
        localDBConfig = Map(
          "dataSource.user" -> "postgres",
          "dataSource.password" -> "postgres",
          "dataSource.url" -> "jdbc:postgresql://localhost:5434/civil_main"
        )
        config = ConfigFactory.parseMap(
          localDBConfig
            .updated(
              "dataSourceClassName",
              "org.postgresql.ds.PGSimpleDataSource"
            )
            .asJava
        )
      } yield Quill.DataSource.fromConfig(config).orDie
    }.flatten

  import io.getquill.MappedEncoding

  implicit val appreciationActionEncoder: MappedEncoding[LikeAction, String] =
    MappedEncoding {
      case LikedState    => "Like"
      case DislikedState => "Dislike"
      case NeutralState  => "Neutral"

    }

  implicit val appreciationActionDecoder: MappedEncoding[String, LikeAction] =
    MappedEncoding {
      case "Like"    => LikedState
      case "Dislike" => DislikedState
      case "Neutral" => NeutralState
    }

}

object DiscussionQueries {

  import QuillContext._

  case class DiscussionsData(
      id: UUID,
      createdAt: ZonedDateTime,
      createdByUsername: String,
      createdByUserId: String,
      title: String,
      editorState: String,
      editorTextContent: String,
      evidenceLinks: Option[List[String]],
      likes: Int,
      userUploadedImageUrl: Option[String],
      userUploadedVodUrl: Option[String],
      discussionKeyWords: Seq[String] = Seq(),
      spaceId: UUID,
      discussionId: Option[UUID] = None,
      contentHeight: Option[Float],
      popularityScore: Double,
      reportStatus: String = ReportStatus.CLEAN.entryName,
      userVerificationType: UserVerificationType = NO_VERIFICATION,
      spaceTitle: String,
      spaceCategory: String,
      linkType: Option[LinkType],
      externalContentUrl: Option[String],
      embedId: Option[String],
      thumbImgUrl: Option[String],
      tag: Option[String],
      iconSrc: Option[String],
      userLikeState: Option[LikeAction],
      userFollowState: Boolean,
      commentCount: Int
  )

  val getOneDiscussion = quote {
    (requestingUserId: String, discussionId: UUID) =>
      sql"""
        WITH CommentCounts AS (
          SELECT
              discussion_id,
              COUNT(id) AS comment_count
          FROM
              comments
          GROUP BY
              discussion_id
        )
        SELECT
            d.*,
            s.title as space_title,
            s.category as space_category,
            link_data.link_type,
            link_data.external_content_url,
            link_data.embed_id,
            link_data.thumb_img_url,
            u.tag,
            u.icon_src,
            dl.like_state as user_like_state,
            CASE WHEN df.id IS NOT NULL THEN TRUE ELSE FALSE END AS user_follow_state,
            COALESCE(cc.comment_count, 0) AS comment_count
        FROM
            discussions d
        JOIN
            users u ON d.created_by_user_id = u.user_id
        JOIN
            spaces s on d.space_id = s.id
        LEFT JOIN
            discussion_likes dl ON d.id = dl.discussion_id AND dl.user_id = $requestingUserId
        LEFT JOIN
            discussion_follows df ON d.id = df.followed_discussion_id AND df.user_id = $requestingUserId
        LEFT JOIN
          external_links_discussions link_data on d.id = link_data.discussion_id
        LEFT JOIN
            CommentCounts cc ON d.id = cc.discussion_id
        WHERE d.id = $discussionId
      """.pure.as[Query[DiscussionsData]]
  }

  val getAllUserDiscussions = quote {
    (requestingUserId: String, skip: Int, userId: String) =>
      sql"""
        WITH CommentCounts AS (
          SELECT
              discussion_id,
              COUNT(id) AS comment_count
          FROM
              comments
          GROUP BY
              discussion_id
        )
        SELECT
            d.*,
            s.title as space_title,
            s.category as space_category,
            link_data.link_type,
            link_data.external_content_url,
            link_data.embed_id,
            link_data.thumb_img_url,
            u.tag,
            u.icon_src,
            dl.like_state as user_like_state,
            CASE WHEN df.id IS NOT NULL THEN TRUE ELSE FALSE END AS user_follow_state,
            COALESCE(cc.comment_count, 0) AS comment_count
        FROM
            discussions d
        JOIN
            users u ON d.created_by_user_id = u.user_id
        JOIN
            spaces s on d.space_id = s.id
        LEFT JOIN
            discussion_likes dl ON d.id = dl.discussion_id AND dl.user_id = $requestingUserId
        LEFT JOIN
            discussion_follows df ON d.id = df.followed_discussion_id AND df.user_id = $requestingUserId
        LEFT JOIN
          external_links_discussions link_data on d.id = link_data.discussion_id
        LEFT JOIN
            CommentCounts cc ON d.id = cc.discussion_id
        WHERE d.created_by_user_id = $userId AND d.title != 'General'
        ORDER BY
            d.created_at DESC
        LIMIT 5
        OFFSET $skip
      """.pure.as[Query[DiscussionsData]]
  }

  val getSpaceDiscussionsQuery = quote {
    (requestingUserId: String, skip: Int, spaceId: UUID) =>
      sql"""
        WITH CommentCounts AS (
          SELECT
              discussion_id,
              COUNT(id) AS comment_count
          FROM
              comments
          GROUP BY
              discussion_id
        )
        SELECT
            d.*,
            s.title as space_title,
            s.category as space_category,
            link_data.link_type,
            link_data.external_content_url,
            link_data.embed_id,
            link_data.thumb_img_url,
            u.tag,
            u.icon_src,
            dl.like_state as user_like_state,
            CASE WHEN df.id IS NOT NULL THEN TRUE ELSE FALSE END AS user_follow_state,
            COALESCE(cc.comment_count, 0) AS comment_count
        FROM
            discussions d
        JOIN
            users u ON d.created_by_user_id = u.user_id
        JOIN
            spaces s on d.space_id = s.id
        LEFT JOIN
            discussion_likes dl ON d.id = dl.discussion_id AND dl.user_id = $requestingUserId
        LEFT JOIN
            discussion_follows df ON d.id = df.followed_discussion_id AND df.user_id = $requestingUserId
        LEFT JOIN
          external_links_discussions link_data on d.id = link_data.discussion_id
        LEFT JOIN
            CommentCounts cc ON d.id = cc.discussion_id
        WHERE d.space_id = $spaceId
        ORDER BY
            d.created_at DESC
        LIMIT 5
        OFFSET $skip
      """.pure.as[Query[DiscussionsData]]
  }

  val getAllFollowedDiscussions = quote {
    (requestingUserId: String, skip: Int) =>
      sql"""
        WITH CommentCounts AS (
          SELECT
              discussion_id,
              COUNT(id) AS comment_count
          FROM
              comments
          GROUP BY
              discussion_id
        )
        SELECT
            d.*,
            s.title as space_title,
            s.category as space_category,
            link_data.link_type,
            link_data.external_content_url,
            link_data.embed_id,
            link_data.thumb_img_url,
            u.tag,
            u.icon_src,
            dl.like_state as user_like_state,
            CASE WHEN df.id IS NOT NULL THEN TRUE ELSE FALSE END AS user_follow_state,
            COALESCE(cc.comment_count, 0) AS comment_count
        FROM
            discussions d
        JOIN
            users u ON d.created_by_user_id = u.user_id
        JOIN
            spaces s on d.space_id = s.id
        LEFT JOIN
            discussion_likes dl ON d.id = dl.discussion_id AND dl.user_id = $requestingUserId
        INNER JOIN
            discussion_follows df ON d.id = df.followed_discussion_id AND df.user_id = $requestingUserId
        LEFT JOIN
          external_links_discussions link_data on d.id = link_data.discussion_id
        LEFT JOIN
            CommentCounts cc ON d.id = cc.discussion_id
        WHERE d.title != 'General'
        ORDER BY
            d.created_at DESC
        LIMIT 5
        OFFSET $skip
      """.pure.as[Query[DiscussionsData]]
  }

  val getAllPopularDiscussions = quote {
    (requestingUserId: String, skip: Int) =>
      sql"""
        WITH CommentCounts AS (
          SELECT
              discussion_id,
              COUNT(id) AS comment_count
          FROM
              comments
          GROUP BY
              discussion_id
        )
        SELECT
            d.*,
            s.title as space_title,
            s.category as space_category,
            link_data.link_type,
            link_data.external_content_url,
            link_data.embed_id,
            link_data.thumb_img_url,
            u.tag,
            u.icon_src,
            dl.like_state as user_like_state,
            CASE WHEN df.id IS NOT NULL THEN TRUE ELSE FALSE END AS user_follow_state,
            COALESCE(cc.comment_count, 0) AS comment_count
        FROM
            discussions d
        JOIN
            users u ON d.created_by_user_id = u.user_id
        JOIN
            spaces s on d.space_id = s.id
        LEFT JOIN
            discussion_likes dl ON d.id = dl.discussion_id AND dl.user_id = $requestingUserId
        LEFT JOIN
            discussion_follows df ON d.id = df.followed_discussion_id AND df.user_id = $requestingUserId
        LEFT JOIN
          external_links_discussions link_data on d.id = link_data.discussion_id
        LEFT JOIN
            CommentCounts cc ON d.id = cc.discussion_id
        WHERE d.title != 'General'
        ORDER BY
            d.popularity_score DESC
        LIMIT 5
        OFFSET $skip
      """.pure.as[Query[DiscussionsData]]
  }

}

object QuillContextQueries {

  import QuillContext._

  val getLikeRatio = quote { (discussionId: UUID) =>
    sql"""
      SELECT
        (SELECT CASE
                    WHEN COUNT(*) = 0 THEN 1
                    ELSE COUNT(*)
                END
         FROM discussion_likes
         WHERE discussion_id = $discussionId AND like_state = 'LikedState')::float /
        (SELECT CASE
                    WHEN COUNT(*) = 0 THEN 1
                    ELSE COUNT(*)
                END
         FROM discussion_likes
         WHERE discussion_id = $discussionId AND like_state = 'DislikedState') AS ratio
         """.pure.as[Query[Float]]

  }

  case class SpacesData(
      id: UUID,
      title: String,
      createdByUserId: String,
      createdByUsername: String,
      editorState: String,
      editorTextContent: String,
      likes: Int,
      category: String,
      reportStatus: String = CLEAN.entryName,
      userVerificationType: UserVerificationType = NO_VERIFICATION,
      createdAt: ZonedDateTime,
      updatedAt: ZonedDateTime,
      spaceId: Option[UUID] = None,
      discussionId: Option[UUID] = None,
      referenceLinks: Option[List[String]] = None,
      contentHeight: Option[Float],
      userUploadedImageUrl: Option[String],
      tag: Option[String],
      iconSrc: Option[String],
      userLikeState: Option[LikeAction],
      userFollowState: Boolean,
      discussionCount: Int,
      commentCount: Int
  )

  val getAllSpacesQuery = quote { (requestingUserId: String, skip: Int) =>
    sql"""
   WITH DiscussionCounts AS (
        SELECT
            space_id,
            COUNT(id) AS discussion_count
        FROM
            discussions
        GROUP BY
            space_id
    ),

    CommentCounts AS (
        SELECT
            space_id,
            COUNT(id) AS comment_count
        FROM
            comments
        GROUP BY
            space_id
    )

    SELECT
        s.*,
        u.tag,
        u.icon_src,
        sl.like_state as user_like_state,
        CASE WHEN sf.id IS NOT NULL THEN TRUE ELSE FALSE END AS user_follow_state,
        COALESCE(dc.discussion_count, 0) AS discussion_count,
        COALESCE(cc.comment_count, 0) AS comment_count
    FROM
        spaces s
    JOIN
        users u ON s.created_by_user_id = u.user_id
    LEFT JOIN
        space_likes sl ON s.id = sl.space_id AND sl.user_id = $requestingUserId
    LEFT JOIN
        space_follows sf ON s.id = sf.followed_space_id AND sf.user_id = $requestingUserId
    LEFT JOIN
        DiscussionCounts dc ON s.id = dc.space_id
    LEFT JOIN
        CommentCounts cc ON s.id = cc.space_id
    ORDER BY
        s.created_at DESC
    LIMIT 5
    OFFSET $skip
  """.pure.as[Query[SpacesData]]
  }

  val getAllFollowedSpacesQuery = quote {
    (requestingUserId: String, skip: Int) =>
      sql"""
   WITH DiscussionCounts AS (
        SELECT
            space_id,
            COUNT(id) AS discussion_count
        FROM
            discussions
        GROUP BY
            space_id
    ),

    CommentCounts AS (
        SELECT
            space_id,
            COUNT(id) AS comment_count
        FROM
            comments
        GROUP BY
            space_id
    )

    SELECT
        s.*,
        u.tag,
        u.icon_src,
        sl.like_state as user_like_state,
        CASE WHEN sf.id IS NOT NULL THEN TRUE ELSE FALSE END AS user_follow_state,
        COALESCE(dc.discussion_count, 0) AS discussion_count,
        COALESCE(cc.comment_count, 0) AS comment_count
    FROM
        spaces s
    JOIN
        users u ON s.created_by_user_id = u.user_id
    LEFT JOIN
        space_likes sl ON s.id = sl.space_id AND sl.user_id = $requestingUserId
    INNER JOIN
        space_follows sf ON s.id = sf.followed_space_id AND sf.user_id = $requestingUserId
    LEFT JOIN
        DiscussionCounts dc ON s.id = dc.space_id
    LEFT JOIN
        CommentCounts cc ON s.id = cc.space_id
    ORDER BY
        s.created_at DESC
    LIMIT 5
    OFFSET $skip
  """.pure.as[Query[SpacesData]]
  }

  val getAllUserSpacesQuery = quote {
    (requestingUserId: String, skip: Int, userId: String) =>
      sql"""
   WITH DiscussionCounts AS (
        SELECT
            space_id,
            COUNT(id) AS discussion_count
        FROM
            discussions
        GROUP BY
            space_id
    ),

    CommentCounts AS (
        SELECT
            space_id,
            COUNT(id) AS comment_count
        FROM
            comments
        GROUP BY
            space_id
    )

    SELECT
        s.*,
        u.tag,
        u.icon_src,
        sl.like_state as user_like_state,
        CASE WHEN sf.id IS NOT NULL THEN TRUE ELSE FALSE END AS user_follow_state,
        COALESCE(dc.discussion_count, 0) AS discussion_count,
        COALESCE(cc.comment_count, 0) AS comment_count
    FROM
        spaces s
    JOIN
        users u ON s.created_by_user_id = u.user_id
    LEFT JOIN
        space_likes sl ON s.id = sl.space_id AND sl.user_id = $requestingUserId
    LEFT JOIN
        space_follows sf ON s.id = sf.followed_space_id AND sf.user_id = $requestingUserId
    LEFT JOIN
        DiscussionCounts dc ON s.id = dc.space_id
    LEFT JOIN
        CommentCounts cc ON s.id = cc.space_id
    WHERE s.created_by_user_id = ${userId}
    ORDER BY
        s.created_at DESC
    LIMIT 5
    OFFSET $skip
  """.pure.as[Query[SpacesData]]
  }

  val getCommentsWithReplies = quote { (id: UUID, userId: String) =>
    sql"""
     WITH RECURSIVE comments_tree as (
      select
        c1.id,
        c1.editor_state,
        c1.created_by_username,
        c1.created_by_user_id,
        c1.sentiment,
        c1.discussion_id,
        c1.parent_id,
        c1.created_at,
        c1.likes,
        c1.root_id,
        c1.source,
        c1.report_status,
        c1.toxicity_status,
        0 as depth,
        u1.icon_src as user_icon_src,
        u1.experience as user_experience,
        u1.user_id,
        u1.tag as created_by_tag,
        l1.like_state,
        civ1.value as civility
      from comments c1
      join users u1 on c1.created_by_user_id = u1.user_id
      left join comment_likes l1 on c1.id = l1.comment_id AND l1.user_id = $userId
      left join comment_civility civ1 on c1.id = civ1.comment_id AND civ1.user_id = $userId
       where c1.id = $id

       union all

      select
        c2.id,
        c2.editor_state,
        c2.created_by_username,
        c2.created_by_user_id,
        c2.sentiment,
        c2.discussion_id ,
        c2.parent_id,
        c2.created_at,
        c2.likes,
        c2.root_id,
        c2.source,
        c2.report_status,
        c2.toxicity_status,
        depth + 1,
        u2.icon_src as user_icon_src,
        u2.experience as user_experience,
        u2.user_id,
        u2.tag as created_by_tag,
        l2.like_state,
        civ2.value as civility
      from comments c2
      join users u2 on c2.created_by_user_id = u2.user_id
      left join comment_likes l2 on c2.id = l2.comment_id AND l2.user_id = $userId
      left join comment_civility civ2 on c2.id = civ2.comment_id AND civ2.user_id = $userId
      join comments_tree ct on ct.id = c2.parent_id

    ) select * from comments_tree
      """.pure.as[Query[CommentWithDepthAndUser]]
  }

  val getTribunalCommentsWithReplies = quote { (id: UUID, userId: String) =>
    sql"""
     WITH RECURSIVE comments_tree as (
      select
        c1.id,
        c1.editor_state,
        c1.created_by_username,
        c1.created_by_user_id,
        c1.sentiment,
        c1.reported_content_id,
        c1.parent_id,
        c1.created_at,
        c1.likes,
        c1.root_id,
        c1.source,
        c1.comment_type,
        0 as depth,
        u1.icon_src as user_icon_src,
        u1.experience as user_experience,
        u1.user_id,
        u1.tag as created_by_tag,
        l1.like_state,
        civ1.value as civility
      from tribunal_comments c1
      join users u1 on c1.created_by_user_id = u1.user_id
      left join comment_likes l1 on c1.id = l1.comment_id AND l1.user_id = $userId
      left join comment_civility civ1 on c1.id = civ1.comment_id AND civ1.user_id = $userId
       where c1.id = $id

       union all

      select
        c2.id,
        c2.editor_state,
        c2.created_by_username,
        c2.created_by_user_id,
        c2.sentiment,
        c2.reported_content_id,
        c2.parent_id,
        c2.created_at,
        c2.likes,
        c2.root_id,
        c2.source,
        c2.comment_type,
        depth + 1,
        u2.icon_src as user_icon_src,
        u2.experience as user_experience,
        u2.user_id,
        u2.tag as created_by_tag,
        l2.like_state,
        civ2.value as civility
      from tribunal_comments c2
      join users u2 on c2.created_by_user_id = u2.user_id
      left join comment_likes l2 on c2.id = l2.comment_id AND l2.user_id = $userId
      left join comment_civility civ2 on c2.id = civ2.comment_id AND civ2.user_id = $userId
      join comments_tree ct on ct.id = c2.parent_id

    ) select * from comments_tree
      """.pure.as[Query[TribunalCommentWithDepthAndUser]]
  }
}
