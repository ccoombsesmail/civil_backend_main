package civil.repositories

import civil.models.enums.{ReportStatus, TopicCategories}
import io.getquill.PostgresZioJdbcContext

import java.sql.Types.{OTHER, VARCHAR}


trait QuillCodecs {

  this: PostgresZioJdbcContext[_] =>


  implicit val topicCategoriesDecoder: Decoder[TopicCategories] =
    decoder(row => index => TopicCategories.withNameInsensitive(row.getObject(index).toString))
  implicit val topicCategoriesEncoder: Encoder[TopicCategories] =
    encoder(VARCHAR, (index, value, row) => row.setObject(index, value, OTHER))

  implicit val reportStatusDecoder: Decoder[ReportStatus] =
    decoder(row => index => ReportStatus.withNameInsensitive(row.getObject(index).toString))
  implicit val reportStatusEncoder: Encoder[ReportStatus] =
    encoder(VARCHAR, (index, value, row) => row.setObject(index, value, OTHER))

    // implicit val localDateTimeDecoder: Decoder[LocalDateTime] =
    //   decoder((index, row, _) => row.getTimestamp(index).toLocalDateTime)

    // implicit val localDateTimeEncoder: Encoder[LocalDateTime] =
    //   encoder(java.sql.Types.TIMESTAMP, (index, value, row) => row.setTimestamp(index, java.sql.Timestamp.valueOf(value)))

}
