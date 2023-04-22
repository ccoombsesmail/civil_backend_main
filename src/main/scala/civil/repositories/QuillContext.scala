package civil.repositories
import civil.models.actions._
import civil.models.{CommentWithDepthAndUser, TribunalCommentWithDepth, TribunalCommentWithDepthAndUser}
import com.typesafe.config.ConfigFactory
import io.getquill.context.ZioJdbc.DataSourceLayer
import io.getquill.{PostgresZioJdbcContext, Query, SnakeCase}
import zio.{System, ZLayer}

import java.util.UUID
import javax.sql.DataSource
import scala.jdk.CollectionConverters.MapHasAsJava


object QuillContext extends PostgresZioJdbcContext(SnakeCase) with QuillCodecs {
  val dataSourceLayer: ZLayer[Any, Nothing, DataSource] =
    ZLayer {
      for {
        herokuURL <- System.env("DATABASE_URL").orDie
        localDBConfig = Map(
          "dataSource.user"     -> "postgres",
          "dataSource.password" -> "postgres",
          "dataSource.url"      -> "jdbc:postgresql://localhost:5434/civil_main"
        )
        config = ConfigFactory.parseMap(
          localDBConfig.updated("dataSourceClassName", "org.postgresql.ds.PGSimpleDataSource").asJava
        )
      } yield DataSourceLayer.fromConfig(config).orDie
    }.flatten

  import io.getquill.MappedEncoding

  implicit val appreciationActionEncoder: MappedEncoding[LikeAction, String] = MappedEncoding {
    case LikedState => "Like"
    case DislikedState => "Dislike"
    case NeutralState => "Neutral"

  }

  implicit val appreciationActionDecoder: MappedEncoding[String, LikeAction] = MappedEncoding {
    case "Like" => LikedState
    case "Dislike" => DislikedState
    case "Neutral" => NeutralState
  }

}


object QuillContextQueries {
  import QuillContext._
  val getCommentsWithReplies = quote { (id: UUID, userId: String) =>
    sql"""
     WITH RECURSIVE comments_tree as (
      select
        c1.id,
        c1.editor_state,
        c1.created_by_username,
        c1.created_by_user_id,
        c1.sentiment,
        c1.discussion_id,
        c1.parent_id,
        c1.created_at,
        c1.likes,
        c1.root_id,
        c1.source,
        c1.report_status,
        c1.toxicity_status,
        0 as depth,
        u1.icon_src as user_icon_src,
        u1.experience as user_experience,
        u1.user_id,
        u1.tag as created_by_tag,
        l1.like_state,
        civ1.value as civility
      from comments c1
      join users u1 on c1.created_by_user_id = u1.user_id
      left join comment_likes l1 on c1.id = l1.comment_id AND l1.user_id = $userId
      left join comment_civility civ1 on c1.id = civ1.comment_id AND civ1.user_id = $userId
       where c1.id = $id

       union all

      select
        c2.id,
        c2.editor_state,
        c2.created_by_username,
        c2.created_by_user_id,
        c2.sentiment,
        c2.discussion_id ,
        c2.parent_id,
        c2.created_at,
        c2.likes,
        c2.root_id,
        c2.source,
        c2.report_status,
        c2.toxicity_status,
        depth + 1,
        u2.icon_src as user_icon_src,
        u2.experience as user_experience,
        u2.user_id,
        u2.tag as created_by_tag,
        l2.like_state,
        civ2.value as civility
      from comments c2
      join users u2 on c2.created_by_user_id = u2.user_id
      left join comment_likes l2 on c2.id = l2.comment_id AND l2.user_id = $userId
      left join comment_civility civ2 on c2.id = civ2.comment_id AND civ2.user_id = $userId
      join comments_tree ct on ct.id = c2.parent_id

    ) select * from comments_tree
      """.pure.as[Query[CommentWithDepthAndUser]]
  }

  val getTribunalCommentsWithReplies = quote { (id: UUID, userId: String) =>
    sql"""
     WITH RECURSIVE comments_tree as (
      select
        c1.id,
        c1.editor_state,
        c1.created_by_username,
        c1.created_by_user_id,
        c1.sentiment,
        c1.reported_content_id,
        c1.parent_id,
        c1.created_at,
        c1.likes,
        c1.root_id,
        c1.source,
        c1.comment_type,
        0 as depth,
        u1.icon_src as user_icon_src,
        u1.experience as user_experience,
        u1.user_id,
        u1.tag as created_by_tag,
        l1.like_state,
        civ1.value as civility
      from tribunal_comments c1
      join users u1 on c1.created_by_user_id = u1.user_id
      left join comment_likes l1 on c1.id = l1.comment_id AND l1.user_id = $userId
      left join comment_civility civ1 on c1.id = civ1.comment_id AND civ1.user_id = $userId
       where c1.id = $id

       union all

      select
        c2.id,
        c2.editor_state,
        c2.created_by_username,
        c2.created_by_user_id,
        c2.sentiment,
        c2.reported_content_id,
        c2.parent_id,
        c2.created_at,
        c2.likes,
        c2.root_id,
        c2.source,
        c2.comment_type,
        depth + 1,
        u2.icon_src as user_icon_src,
        u2.experience as user_experience,
        u2.user_id,
        u2.tag as created_by_tag,
        l2.like_state,
        civ2.value as civility
      from tribunal_comments c2
      join users u2 on c2.created_by_user_id = u2.user_id
      left join comment_likes l2 on c2.id = l2.comment_id AND l2.user_id = $userId
      left join comment_civility civ2 on c2.id = civ2.comment_id AND civ2.user_id = $userId
      join comments_tree ct on ct.id = c2.parent_id

    ) select * from comments_tree
      """.pure.as[Query[TribunalCommentWithDepthAndUser]]
  }
}
