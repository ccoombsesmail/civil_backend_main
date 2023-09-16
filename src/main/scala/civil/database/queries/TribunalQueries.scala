package civil.database.queries

import civil.database.queries.DiscussionQueries.SimilarDiscussionsData
import civil.repositories.QuillContext
import io.getquill.Query

object TribunalQueries {

  import QuillContext._

  val getUserJuryDutiesQuery = quote { (userId: String) =>
    sql"""
     SELECT
          tjm.content_type,
          tjm.content_id,
          tjm.jury_duty_completion_time,
          CASE
              WHEN tjm.content_type = 'SPACE' THEN s.title
              WHEN tjm.content_type = 'DISCUSSION' THEN d.title
              WHEN tjm.content_type = 'COMMENT' THEN c.editor_text_content
          END AS content_title,
          CASE
              WHEN tjm.content_type = 'SPACE' THEN s.created_by_username
              WHEN tjm.content_type = 'DISCUSSION' THEN d.created_by_username
              WHEN tjm.content_type = 'COMMENT' THEN c.created_by_username
          END AS created_by_username,
          CASE
           WHEN tjm.content_type = 'SPACE' THEN s.created_by_id
           WHEN tjm.content_type = 'DISCUSSION' THEN d.created_by_id
           WHEN tjm.content_type = 'COMMENT' THEN c.created_by_id
          END AS created_by_id,
          CASE
              WHEN tjm.content_type = 'SPACE' THEN u1.tag
              WHEN tjm.content_type = 'DISCUSSION' THEN u2.tag
              WHEN tjm.content_type = 'COMMENT' THEN u3.tag
          END AS created_by_tag,
          CASE
            WHEN tjm.content_type = 'SPACE' THEN u1.icon_src
            WHEN tjm.content_type = 'DISCUSSION' THEN u2.icon_src
            WHEN tjm.content_type = 'COMMENT' THEN u3.icon_src
          END AS created_by_icon_src,
          CASE
              WHEN tjm.content_type = 'SPACE' THEN s.userUploadedImageUrl
              WHEN tjm.content_type = 'SPACE' THEN s.userUploadedImageUrl
              ELSE NULL
          END AS user_uploaded_image_url
          CASE
              WHEN tjm.content_type = 'DISCUSSION' THEN eld.external_content_url
              ELSE NULL
          END AS external_content_url
      FROM
          tribunal_jury_members tjm
      LEFT JOIN spaces s ON tjm.content_id = s.id AND tjm.content_type = 'Space'
      LEFT JOIN discussions d ON tjm.content_id = d.id AND tjm.content_type = 'Discussion'
      LEFT JOIN comments c ON tjm.content_id = c.id AND tjm.content_type = 'Comment'
      LEFT JOIN users u1 ON s.created_by_user_id = u1.user_id
      LEFT JOIN users u2 ON d.created_by_user_id = u2.user_id
      LEFT JOIN users u3 ON c.created_by_user_id = u3.user_id
      LEFT JOIN external_links_discussions eld ON d.id = eld.discussion_id
      WHERE
          tjm.user_id = $userId

           """.pure.as[Query[SimilarDiscussionsData]]
  }

}
