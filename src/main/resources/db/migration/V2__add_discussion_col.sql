ALTER TABLE discussions ADD COLUMN popularity_score DOUBLE PRECISION DEFAULT 0;


UPDATE discussions
SET popularity_score = (
    ((COALESCE(ldc.like_count, 0) + 1.0) / (COALESCE(ldc.dislike_count, 0) + 1.0)) *
    EXTRACT(EPOCH FROM NOW() - discussions.created_at) *
    (COALESCE(cmt.comment_count, 0) + 1)
)
FROM
    (SELECT
        discussion_id,
        COUNT(like_state = 'LikedState' OR NULL) AS like_count,
        COUNT(like_state = 'DislikedState' OR NULL) AS dislike_count
    FROM discussion_likes
    GROUP BY discussion_id) AS ldc
FULL OUTER JOIN
    (SELECT
        discussion_id,
        COUNT(*) AS comment_count
    FROM comments
    GROUP BY discussion_id) AS cmt ON ldc.discussion_id = cmt.discussion_id
WHERE discussions.id = COALESCE(ldc.discussion_id, cmt.discussion_id);