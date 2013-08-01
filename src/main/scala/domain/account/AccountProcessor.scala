package domain.account

import akka.actor.{ActorRef, Actor}
import domain._
import domain.OpenAccount
import domain.DepositMoney
import domain.AccountId
import cqrs.{EventStream}
import util.{EventsCommitted, AppendEventsToStream, LoadEventStream}
import com.weiglewilczek.slf4s.Logger


class AccountProcessor(val eventStore: ActorRef) extends Actor {
  val logger = Logger("ApplicationService")

  val factory = new AccountFactory()

  def receive = {
    case cmd: AccountCommand => eventStore ! LoadEventStream(cmd.id.id, cmd)
    case stream: EventStream => process(stream.boomerang.asInstanceOf[AccountCommand], stream)
    case commit: EventsCommitted =>
    case msg => logger.error("Unknown message " + msg)
  }

  def process(command: AccountCommand, stream: EventStream) = command match {
    case cmd: OpenAccount => insert(stream, cmd.id, (Unit) => factory.create(cmd.id, cmd.currency))
    case cmd: DepositMoney => update(stream, cmd.id, (a:Account) => a.depositMoney(cmd.amount))
    case cmd: RequestMoneyWithdrawal => update(stream, cmd.id, (a:Account) => a.requestMoneyWithdrawal(cmd.transactionId, cmd.amount))
    case cmd: ConfirmMoneyWithdrawal => update(stream, cmd.id, (a:Account) => a.confirmMoneyWithdrawal(cmd.transactionId))
  }

  def insert(stream: EventStream, id: AccountId, f: Unit => Account) {
    if (stream.streamVersion == 0) {
      val aggregate = f()
      eventStore ! AppendEventsToStream(id.id, aggregate.version, aggregate.uncommittedEvents)
    } else
      throw new RuntimeException("account with this id already exists " + id)
  }

  def update(stream: EventStream, id: AccountId, f: Account => Account) {
    if (stream.streamVersion != 0) {
      val restoredAggr = factory.restoreFromHistory(stream.events.asInstanceOf[List[AccountEvent]])
      val newAggregate = f(restoredAggr)
      eventStore ! AppendEventsToStream(id.id, newAggregate.version, newAggregate.uncommittedEvents)
    } else
      throw new RuntimeException("account with this id does not exist " + id)
  }




}
