package civil.repositories

import civil.models.actions.LikeAction
import civil.models.enums.{ReportStatus, SpaceCategories}
import io.getquill.PostgresZioJdbcContext

import java.sql.Timestamp
import java.sql.Types.{OTHER, VARCHAR}
import java.time.{LocalDateTime, ZonedDateTime}

trait QuillCodecs {

  this: PostgresZioJdbcContext[_] =>

  implicit val topicCategoriesDecoder: Decoder[SpaceCategories] =
    decoder(row =>
      index =>
        SpaceCategories.withNameInsensitive(row.getObject(index).toString)
    )
  implicit val topicCategoriesEncoder: Encoder[SpaceCategories] =
    encoder(VARCHAR, (index, value, row) => row.setObject(index, value, OTHER))

  implicit val reportStatusDecoder: Decoder[ReportStatus] =
    decoder(row =>
      index => ReportStatus.withNameInsensitive(row.getObject(index).toString)
    )
  implicit val reportStatusEncoder: Encoder[ReportStatus] =
    encoder(VARCHAR, (index, value, row) => row.setObject(index, value, OTHER))

  implicit val likeStateDecoder: Decoder[LikeAction] =
    decoder(row => index => LikeAction.withName(row.getObject(index).toString))
  implicit val likeStateEncoder: Encoder[LikeAction] =
    encoder(VARCHAR, (index, value, row) => row.setObject(index, value, OTHER))


  //  implicit val localDateTimeEncoder: Encoder[LocalDateTime] =
  //    encoder(
  //      java.sql.Types.TIMESTAMP,
  //      (index, value, row) =>
  //        row.setTimestamp(index, java.sql.Timestamp.valueOf(value))
  //    )

}
