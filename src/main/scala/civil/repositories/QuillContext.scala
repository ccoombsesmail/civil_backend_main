package civil.repositories

import civil.models.actions._
import civil.models.Reports
import com.typesafe.config.ConfigFactory
import cats.implicits.catsSyntaxOptionId

import io.getquill.jdbczio.Quill
import io.getquill.{PostgresZioJdbcContext, Query, SnakeCase}
import zio._

import java.util.UUID
import javax.sql.DataSource
import scala.jdk.CollectionConverters.MapHasAsJava

object QuillContext extends PostgresZioJdbcContext(SnakeCase) with QuillCodecs {

  val dataSourceLayer: ZLayer[Any, Nothing, DataSource] =
    ZLayer {
      for {
        dbUrl <- System.env("DATABASE_URL").orElse(ZIO.succeed(Option.empty[String]))
        fullUrl = dbUrl match {
          case Some(value) => s"jdbc:postgresql://${value}:5432/civil_main"
          case None        => "jdbc:postgresql://localhost:5434/civil_main"
        }
        dbPassword <- System.env("DATABASE_PASSWORD").orElse(ZIO.succeed("password".some))
        _ <- ZIO.logInfo(s"Connection to database: ${System.env("DATABASE_URL")}")
        _ <- ZIO.logInfo(s"Full Url: ${fullUrl}")
        localDBConfig = Map(
          "dataSource.user" -> "postgres",
          "dataSource.password" -> dbPassword.getOrElse("postgres"),
          "dataSource.url" -> fullUrl
        )
        config = ConfigFactory.parseMap(
          localDBConfig
            .updated(
              "dataSourceClassName",
              "org.postgresql.ds.PGSimpleDataSource"
            )
            .asJava
        )
      } yield Quill.DataSource.fromConfig(config).tapError(e => {
        ZIO.logInfo(s"Error Creating DataSource: $e")
      }).orDie
    }.flatten

  import io.getquill.MappedEncoding

  implicit val appreciationActionEncoder: MappedEncoding[LikeAction, String] =
    MappedEncoding {
      case LikedState    => "Like"
      case DislikedState => "Dislike"
      case NeutralState  => "Neutral"

    }

  implicit val appreciationActionDecoder: MappedEncoding[String, LikeAction] =
    MappedEncoding {
      case "Like"    => LikedState
      case "Dislike" => DislikedState
      case "Neutral" => NeutralState
    }

}

object ReportQueries {

  import QuillContext._

  case class ReportCountBySeverity(severity: String, count: Int)

  case class ReportCountByCause(cause: String, count: Int)

  def getReportCountsBySeverity(contentId: UUID) = {
    val q = quote {
      query[Reports]
        .filter(_.contentId == lift(contentId))
        .groupBy(_.severity)
        .map { case (severity, reports) =>
          (severity, reports.size)
        }
    }
    run(q)
  }

  def getReportCountsByCause(contentId: UUID) = {
    val q = quote {
      query[Reports]
        .filter(_.contentId == lift(contentId))
        .groupBy(_.reportCause)
        .map { case (cause, reports) =>
          (cause, reports.size)
        }
    }
    run(q)
  }

  //  val getReportCountsBySeverity = quote { (contentId: UUID) =>
  //    sql"""
  //    SELECT severity, COUNT(*) as count
  //    FROM reports
  //    WHERE content_id = $contentId
  //    GROUP BY severity;
  //          """.pure.as[Query[ReportCountBySeverity]]
  //  }
  //
  //  val getReportCountsByCause = quote { (contentId: UUID) =>
  //    sql"""
  //    SELECT report_cause, COUNT(*) as count
  //    FROM reports
  //    WHERE content_id = $contentId
  //    GROUP BY report_cause;
  //          """.pure.as[Query[ReportCountByCause]]
  //  }

}

object QuillContextQueries {

  import QuillContext._

  val getLikeRatio = quote { (discussionId: UUID) =>
    sql"""
      SELECT
        (SELECT CASE
                    WHEN COUNT(*) = 0 THEN 1
                    ELSE COUNT(*)
                END
         FROM discussion_likes
         WHERE discussion_id = $discussionId AND like_state = 'LikedState')::float /
        (SELECT CASE
                    WHEN COUNT(*) = 0 THEN 1
                    ELSE COUNT(*)
                END
         FROM discussion_likes
         WHERE discussion_id = $discussionId AND like_state = 'DislikedState') AS ratio
         """.pure.as[Query[Float]]

  }

}
