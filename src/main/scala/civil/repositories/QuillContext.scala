package civil.repositories
import cats.conversions.all.autoWidenFunctor
import civil.config.Config
import civil.models.{CommentWithDepth, CommentWithDepthAndUser, TribunalCommentWithDepth}
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import io.getquill.{PostgresJdbcContext, PostgresZioJdbcContext, Query, SnakeCase}
import io.circe._
import io.circe.parser._
import scalaz.Leibniz.subst
import zio.ZIO
import zio.blocking.Blocking

import java.sql.Types
import java.sql.Timestamp
import java.util.UUID
import java.time.Instant

class QuillContext extends PostgresZioJdbcContext(SnakeCase) with QuillCodecs

object QuillContextHelper extends QuillContext {
  lazy val pgDataSource = new org.postgresql.ds.PGSimpleDataSource()
  val pass = Config().getString("civil.pass")
  pgDataSource.setUser("postgres")
  pgDataSource.setPassword(pass)
  println(Config().getString("civil.databaseUrl"))
  pgDataSource.setUrl(Config().getString("civil.databaseUrl"))
  val config = new HikariConfig()
  config.setDataSource(pgDataSource)
  lazy val ctx = new PostgresJdbcContext(SnakeCase, new HikariDataSource(config))



}


object QuillContextQueries extends QuillContext {
  val getCommentsWithReplies = quote { (id: UUID) =>
    infix"""
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
        u1.user_id
      from comments c1
      join users u1 on c1.created_by_user_id = u1.user_id
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
        u2.user_id
      from comments c2
      join users u2 on c2.created_by_user_id = u2.user_id
      join comments_tree ct on ct.id = c2.parent_id

    ) select * from comments_tree
      """.as[Query[CommentWithDepthAndUser]]
//    infix"""
//     WITH RECURSIVE comments_tree as (
//      select
//        c1.id,
//        c1.editor_state,
//        c1.created_by_username,
//        c1.created_by_user_id,
//        c1.sentiment,
//        c1.discussion_id,
//        c1.parent_id,
//        c1.created_at,
//        c1.likes,
//        c1.root_id,
//        c1.source,
//        c1.report_status,
//        c1.toxicity_status,
//        0 as depth
//      from comments c1
//       where c1.id = $id
//
//       union all
//
//      select
//        c2.id,
//        c2.editor_state,
//        c2.created_by_username,
//        c2.created_by_user_id,
//        c2.sentiment,
//        c2.discussion_id ,
//        c2.parent_id,
//        c2.created_at,
//        c2.likes,
//        c2.root_id,
//        c2.source,
//        c2.report_status,
//        c2.toxicity_status,
//        depth + 1
//      from comments c2
//      join comments_tree ct on ct.id = c2.parent_id
//
//    ) select * from comments_tree
//      """.as[Query[CommentWithDepth]]
  }

  val getTribunalCommentsWithReplies = quote { (id: UUID) =>
    infix"""
     WITH RECURSIVE comments_tree as (
      select
        c1.id,
        c1.editor_state,
        c1.created_by_username,
        c1.sentiment,
        c1.reported_content_id,
        c1.parent_id,
        c1.created_at,
        c1.likes,
        c1.root_id,
        c1.source,
        c1.comment_type,
        0 as depth
      from tribunal_comments c1
       where c1.id = $id

       union all

      select
        c2.id,
        c2.editor_state,
        c2.created_by_username,
        c2.sentiment,
        c2.reported_content_id,
        c2.parent_id,
        c2.created_at,
        c2.likes,
        c2.root_id,
        c2.source,
        c2.comment_type,
        depth + 1
      from tribunal_comments c2
      join comments_tree ct on ct.id = c2.parent_id

    ) select * from comments_tree
      """.as[Query[TribunalCommentWithDepth]]
  }
}
