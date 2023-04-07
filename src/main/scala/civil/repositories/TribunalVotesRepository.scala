package civil.repositories


import civil.errors.AppError
import civil.errors.AppError.InternalServerError
import civil.models.{TribunalVote, TribunalVotes}
import io.scalaland.chimney.dsl.TransformerOps
import zio.{URLayer, ZIO, ZLayer}

trait TribunalVotesRepository {
  def addTribunalVote(tribunalVote: TribunalVotes): ZIO[Any, AppError, TribunalVote]
}



object TribunalVotesRepository {
  def addTopicTribunalVote(tribunalVote: TribunalVotes): ZIO[TribunalVotesRepository, AppError, TribunalVote] =
    ZIO.serviceWithZIO[TribunalVotesRepository](
      _.addTribunalVote(tribunalVote)
    )
}


case class TribunalVotesRepositoryLive() extends TribunalVotesRepository {
  import QuillContextHelper.ctx._


  override def addTribunalVote(tribunalVote: TribunalVotes): ZIO[Any, AppError, TribunalVote] = {
    for {
//      juryMember <- ZIO.effect(run(
//        query[TopicTribunalJury].filter(ttj => ttj.userId == lift(tribunalVote.userId))
//      )).mapError(e => InternalServerError(e.toString))
//      _ <- ZIO.fail(
//        Unauthorized("Must Be Selected As A Jury Member To Vote")
//      ).unless(juryMember.nonEmpty)
      vote <- ZIO.attempt(run(
        query[TribunalVotes]
          .insertValue(lift(tribunalVote))
          .returning(r => r)
      )).mapError(e => InternalServerError(e.toString))
    } yield vote.into[TribunalVote].transform
  }
}



object TribunalVotesRepositoryLive {
  val layer: URLayer[Any, TribunalVotesRepository] = ZLayer.fromFunction(TribunalVotesRepositoryLive.apply _)
}

