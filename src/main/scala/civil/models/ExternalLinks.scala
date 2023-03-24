package civil.models

import civil.models.enums.LinkType

import java.util.UUID

case class ExternalLinks(
    topicId: UUID,
    embedId: Option[String],
    externalContentUrl: String,
    thumbImgUrl: Option[String],
    linkType: LinkType
)

case class ExternalLinksDiscussions(
    discussionId: UUID,
    embedId: Option[String],
    externalContentUrl: String,
    thumbImgUrl: Option[String],
    linkType: LinkType
)
