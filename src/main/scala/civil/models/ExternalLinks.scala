package civil.models

import civil.models.enums.LinkType
import zio.json.{DeriveJsonCodec, JsonCodec}

import java.util.UUID

case class ExternalLinks(
    spaceId: UUID,
    embedId: Option[String],
    externalContentUrl: String,
    thumbImgUrl: Option[String],
    linkType: LinkType
)

object ExternalLinks {
  implicit val codec: JsonCodec[ExternalLinks] = DeriveJsonCodec.gen[ExternalLinks]
}


case class ExternalLinksDiscussions(
    discussionId: UUID,
    embedId: Option[String],
    externalContentUrl: String,
    thumbImgUrl: Option[String],
    linkType: LinkType
)


object ExternalLinksDiscussions {
  implicit val codec: JsonCodec[ExternalLinksDiscussions] = DeriveJsonCodec.gen[ExternalLinksDiscussions]
}

