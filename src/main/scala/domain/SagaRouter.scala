package domain

import akka.actor.{FSM, Props, ActorRef, Actor}
import domain.WaitingForApproval

/**
 * Created with IntelliJ IDEA.
 * User: guenter
 * Date: 03.07.13
 * Time: 06:41
 * To change this template use File | Settings | File Templates.
 */
sealed trait State
case object Idle extends State
case object WaitingForApproval extends State

case object Uninitialized extends Event

class PlaceOrderSaga(accountService: ActorRef, orderBookService: ActorRef) extends Actor with FSM[State, Event] {
  startWith(Idle, Uninitialized)

  when(Idle) {
    case Event(Uninitialized, e : OrderPlaced) =>
      accountService ! WithdrawMoney(e.order.account, e.order.pricePerUnit * e.order.quantity)
      goto (WaitingForApproval)
  }

  when(WaitingForApproval) {
    case Event(_, e : MoneyWithdrawn) => {
      orderBookService ! ComfirmOrderPlacement()
    }
    stay
  }

}

class SagaRouter extends Actor {
  var sagas = Map.empty[String, ActorRef]
  def receive = {
    case e : OrderPlaced => {
      val saga = sagas.getOrElse(e.id.id, context.system.actorOf(new Props(new PlaceOrderSaga())))
      saga ! e
    }
  }
}
