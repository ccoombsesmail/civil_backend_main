package civil.models.enums

import civil.repositories.QuillContext.{Encoder, Index, PrepareRow, encoder}
import org.postgresql.util.PGobject

import java.sql.Types

object EnumEncoders {


  implicit val topicCategoriesEncoder: Encoder[TopicCategories] = encoder[TopicCategories](
    Types.OTHER,
    (index: Index, value: TopicCategories, row: PrepareRow) => {
      val pgObj = new PGobject()
      pgObj.setType("topic_categories")
      pgObj.setValue(value.entryName)
      row.setObject(index, pgObj, Types.OTHER)
    }
  )

  implicit val reportStatusEncoder: Encoder[ReportStatus] = encoder[ReportStatus](
    Types.OTHER,
    (index: Index, value: ReportStatus, row: PrepareRow) => {
      val pgObj = new PGobject()
      pgObj.setType("report_status")
      pgObj.setValue(value.entryName)
      row.setObject(index, pgObj, Types.OTHER)
    }
  )
}
