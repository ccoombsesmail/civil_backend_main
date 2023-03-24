package civil.repositories

import civil.models.{ErrorInfo, InternalServerError, TribunalVote, TribunalVotes}
import io.scalaland.chimney.dsl.TransformerOps
import zio.{Has, ZIO, ZLayer}

trait TribunalVotesRepository {
  def addTribunalVote(tribunalVote: TribunalVotes): ZIO[Any, ErrorInfo, TribunalVote]
}



object TribunalVotesRepository {
  def addTopicTribunalVote(tribunalVote: TribunalVotes): ZIO[Has[TribunalVotesRepository], ErrorInfo, TribunalVote] =
    ZIO.serviceWith[TribunalVotesRepository](
      _.addTribunalVote(tribunalVote)
    )
}


case class TribunalVotesRepositoryLive() extends TribunalVotesRepository {
  import QuillContextHelper.ctx._


  override def addTribunalVote(tribunalVote: TribunalVotes): ZIO[Any, ErrorInfo, TribunalVote] = {
    for {
//      juryMember <- ZIO.effect(run(
//        query[TopicTribunalJury].filter(ttj => ttj.userId == lift(tribunalVote.userId))
//      )).mapError(e => InternalServerError(e.toString))
//      _ <- ZIO.fail(
//        Unauthorized("Must Be Selected As A Jury Member To Vote")
//      ).unless(juryMember.nonEmpty)
      vote <- ZIO.effect(run(
        query[TribunalVotes]
          .insert(lift(tribunalVote))
          .returning(r => r)
      )).mapError(e => InternalServerError(e.toString))
    } yield vote.into[TribunalVote].transform
  }
}



object TribunalVotesRepositoryLive {
  val live: ZLayer[Any, Nothing, Has[TribunalVotesRepository]] = ZLayer.succeed(TribunalVotesRepositoryLive())
}

