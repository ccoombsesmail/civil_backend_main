package civil.database.queries

import civil.models.actions.LikeAction
import civil.models.enums.{LinkType, ReportStatus, UserVerificationType}
import civil.models.enums.UserVerificationType.NO_VERIFICATION
import civil.repositories.QuillContext
import io.getquill.{Query, Quoted}

import java.time.ZonedDateTime
import java.util.UUID

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

  case class DiscussionsDataUnauthenticated(
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
      commentCount: Int
  )

  case class SimilarDiscussionsData(
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
      commentCount: Int
  )

  val getSimilarDiscussionsQuery
      : Quoted[UUID => Query[SimilarDiscussionsData]] = quote {
    (discussionId: UUID) =>
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
         COALESCE(cc.comment_count, 0) AS comment_count
     FROM
         discussions d
     JOIN
         users u ON d.created_by_user_id = u.user_id
     JOIN
         spaces s on d.space_id = s.id
     JOIN
         discussion_similarity_scores dss ON
             (dss.discussion_id1 = $discussionId AND d.id = dss.discussion_id2) OR
             (dss.discussion_id2 = $discussionId AND d.id = dss.discussion_id1)
     LEFT JOIN
         external_links_discussions link_data ON d.id = link_data.discussion_id
     LEFT JOIN
          CommentCounts cc ON d.id = cc.discussion_id
     WHERE
         d.id != $discussionId
     ORDER BY
         dss.similarity_score DESC
     LIMIT 30

          """.pure.as[Query[SimilarDiscussionsData]]
  }

  val getOneDiscussion: Quoted[(String, UUID) => Query[DiscussionsData]] =
    quote { (requestingUserId: String, discussionId: UUID) =>
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

  val getOneDiscussionUnauthenticatedQuery
      : Quoted[UUID => Query[DiscussionsDataUnauthenticated]] =
    quote { (discussionId: UUID) =>
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
           COALESCE(cc.comment_count, 0) AS comment_count
       FROM
           discussions d
       JOIN
           users u ON d.created_by_user_id = u.user_id
       JOIN
           spaces s on d.space_id = s.id
       LEFT JOIN
         external_links_discussions link_data on d.id = link_data.discussion_id
       LEFT JOIN
           CommentCounts cc ON d.id = cc.discussion_id
       WHERE d.id = $discussionId
     """.pure.as[Query[DiscussionsDataUnauthenticated]]
    }

  val getAllUserDiscussions
      : Quoted[(String, Index, String) => Query[DiscussionsData]] = quote {
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

  val getAllUserDiscussionsUnauthenticatdQuery
      : Quoted[(Int, String) => Query[DiscussionsDataUnauthenticated]] = quote {
    (skip: Int, userId: String) =>
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
           COALESCE(cc.comment_count, 0) AS comment_count
       FROM
           discussions d
       JOIN
           users u ON d.created_by_user_id = u.user_id
       JOIN
           spaces s on d.space_id = s.id
       LEFT JOIN
         external_links_discussions link_data on d.id = link_data.discussion_id
       LEFT JOIN
           CommentCounts cc ON d.id = cc.discussion_id
       WHERE d.created_by_user_id = $userId AND d.title != 'General'
       ORDER BY
           d.created_at DESC
       LIMIT 5
       OFFSET $skip
     """.pure.as[Query[DiscussionsDataUnauthenticated]]
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

  val getSpaceDiscussionsUnauthenticatedQuery = quote {
    (skip: Int, spaceId: UUID) =>
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
           COALESCE(cc.comment_count, 0) AS comment_count
       FROM
           discussions d
       JOIN
           users u ON d.created_by_user_id = u.user_id
       JOIN
           spaces s on d.space_id = s.id
       LEFT JOIN
         external_links_discussions link_data on d.id = link_data.discussion_id
       LEFT JOIN
           CommentCounts cc ON d.id = cc.discussion_id
       WHERE d.space_id = $spaceId
       ORDER BY
           d.created_at DESC
       LIMIT 5
       OFFSET $skip
     """.pure.as[Query[DiscussionsDataUnauthenticated]]
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

  val getAllPopularDiscussions
      : Quoted[(String, Index) => Query[DiscussionsData]] = quote {
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

  val getAllPopularDiscussionsUnauthenticatedQuery = quote { (skip: Int) =>
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
           COALESCE(cc.comment_count, 0) AS comment_count
       FROM
           discussions d
       JOIN
           users u ON d.created_by_user_id = u.user_id
       JOIN
           spaces s on d.space_id = s.id
       LEFT JOIN
         external_links_discussions link_data on d.id = link_data.discussion_id
       LEFT JOIN
           CommentCounts cc ON d.id = cc.discussion_id
       WHERE d.title != 'General'
       ORDER BY
           d.popularity_score DESC
       LIMIT 5
       OFFSET $skip
     """.pure.as[Query[DiscussionsDataUnauthenticated]]
  }
}
