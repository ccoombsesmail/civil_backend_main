package civil.models

case class Follows(
    userId: String,
    followedUserId: String
)

case class FollowedUserId(
    followedUserId: String
)
