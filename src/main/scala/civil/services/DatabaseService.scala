package civil.services

import zio._

import java.io.Closeable
import java.sql.{Connection, SQLException}
import javax.sql.DataSource

trait DatabaseService {
  def withConnection[R, E, A](f: Connection => ZIO[R, E, A]): ZIO[R, Either[SQLException, E], A]
}

object ConnectionService {
  def withConnection[R, E, A](
                               f: Connection => ZIO[R, E, A]
                             ): ZIO[R with Has[DatabaseService], Either[SQLException, E], A] =
    ZIO.service[DatabaseService].flatMap(_.withConnection(f))
}

case class ConnectionServiceLive(
                                  ds: DataSource with Closeable,
                                  poolGuard: Semaphore,
                                ) extends DatabaseService {
  override def withConnection[R, E, A](f: Connection => ZIO[R, E, A]): ZIO[R, Either[SQLException, E], A] =
    for {
      result <- poolGuard.withPermit(usePermit(f))
    } yield result

  private def usePermit[R, E, A](f: Connection => ZIO[R, E, A]) =
    for {
      result <- ZIO
        .effect(ds.getConnection)
        .refineToOrDie[SQLException]
        .mapError(Left.apply)
        .bracket(releaseConnection, useConnection(f))
    } yield result

  private def useConnection[R, E, A](f: Connection => ZIO[R, E, A])(
    conn: Connection
  ): ZIO[R, Right[SQLException, E], A] =
    f(conn).mapError(Right.apply)

  private def releaseConnection(conn: Connection): UIO[Unit] =
    ZIO.effect(conn.close()).catchAll {
      case e: SQLException => ZIO.succeed(println(s"Ignoring SQLException caught when closing a connection."))
      case t => ZIO.succeed(println("Unexpected exception closing a connection.", t))
    }
}

object ConnectionServiceLive {
  val layer: URLayer[Has[DataSource with Closeable] with Has[Semaphore], Has[
    DatabaseService
  ]] =
    (ConnectionServiceLive(_, _)).toLayer
}
