package civil.database.queries

import civil.models.actions.LikeAction
import civil.models.enums.ReportStatus.CLEAN
import civil.models.enums.UserVerificationType
import civil.models.enums.UserVerificationType.NO_VERIFICATION
import civil.repositories.QuillContext
import io.getquill.Query

import java.time.ZonedDateTime
import java.util.UUID

object SpaceQueries {

  import QuillContext._

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

  case class SpacesDataUnauthenticated(
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
      discussionCount: Int,
      commentCount: Int
  )

  val getSpaceQuery = quote { (requestingUserId: String, spaceId: UUID) =>
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
        WHERE s.id = $spaceId
    """.pure.as[Query[SpacesData]]
  }

  val getSpaceQueryUnauthenticated = quote { (spaceId: UUID) =>
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
           COALESCE(dc.discussion_count, 0) AS discussion_count,
           COALESCE(cc.comment_count, 0) AS comment_count
       FROM
           spaces s
       JOIN
           users u ON s.created_by_user_id = u.user_id
       LEFT JOIN
           DiscussionCounts dc ON s.id = dc.space_id
       LEFT JOIN
           CommentCounts cc ON s.id = cc.space_id
        WHERE s.id = $spaceId
    """.pure.as[Query[SpacesDataUnauthenticated]]
  }

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

  val getAllSpacesUnauthenticatedQuery = quote { (skip: Int) =>
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
           COALESCE(dc.discussion_count, 0) AS discussion_count,
           COALESCE(cc.comment_count, 0) AS comment_count
       FROM
           spaces s
       JOIN
           users u ON s.created_by_user_id = u.user_id
       LEFT JOIN
           DiscussionCounts dc ON s.id = dc.space_id
       LEFT JOIN
           CommentCounts cc ON s.id = cc.space_id
       ORDER BY
           s.created_at DESC
       LIMIT 5
       OFFSET $skip
    """.pure.as[Query[SpacesDataUnauthenticated]]
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

  val getAllUserSpacesUnauthenticatedQuery = quote {
    (skip: Int, userId: String) =>
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
             COALESCE(dc.discussion_count, 0) AS discussion_count,
             COALESCE(cc.comment_count, 0) AS comment_count
         FROM
             spaces s
         JOIN
             users u ON s.created_by_user_id = u.user_id
         LEFT JOIN
             DiscussionCounts dc ON s.id = dc.space_id
         LEFT JOIN
             CommentCounts cc ON s.id = cc.space_id
         WHERE s.created_by_user_id = ${userId}
         ORDER BY
             s.created_at DESC
         LIMIT 5
         OFFSET $skip
      """.pure.as[Query[SpacesDataUnauthenticated]]
  }
}
