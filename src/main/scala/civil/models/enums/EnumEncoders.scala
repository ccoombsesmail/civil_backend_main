package civil.models.enums

import civil.repositories.QuillContext.{Encoder, Index, PrepareRow, encoder}
import org.postgresql.util.PGobject

import java.sql.Types

object EnumEncoders {


  implicit val spaceCategoriesEncoder: Encoder[SpaceCategories] = encoder[SpaceCategories](
    Types.OTHER,
    (index: Index, value: SpaceCategories, row: PrepareRow) => {
      val pgObj = new PGobject()
      pgObj.setType("space_categories")
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
