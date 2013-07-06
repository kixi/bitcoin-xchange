package domain.sagas

import akka.actor._
import domain._
import domain.sagas.SagaData
import domain.OrderPlaced
import domain.LimitOrder
import domain.AccountId
import domain.OrderBookId
import akka.actor.FSM.Normal
import domain.sagas.SagaData
import domain.RequestMoneyWithdrawal
import domain.OrderPlaced
import domain.LimitOrder
import domain.AccountId
import domain.PrepareOrderPlacement
import domain.OrderPlacementPrepared
import domain.ConfirmMoneyWithdrawal
import domain.OrderBookId
import domain.MoneyWithdrawalConfirmed
import domain.MoneyWithdrawalRequested
import domain.TransactionId
import domain.OrderPlacementConfirmed
import com.weiglewilczek.slf4s.Logger

/**
 * Created with IntelliJ IDEA.
 * User: guenter
 * Date: 03.07.13
 * Time: 06:41
 * To change this template use File | Settings | File Templates.
 */
sealed trait State
case object Start extends State
case object WaitingForAccountApproval extends State
case object WaitingForOrderBookApproval extends State
case object WaitingForAccountConfirmation extends State
case object WaitingForOrderBookConfirmation extends State

sealed trait Data
case object Uninitialized extends Data
case class SagaData(orderBookId: OrderBookId, order:LimitOrder, accountId: AccountId, transactionId: TransactionId ) extends Data

class PlaceOrderSaga(commandReceiver: ActorRef) extends Actor with LoggingFSM[State, Data] {
  startWith(Start, Uninitialized)

  when(Start) {
    case Event(e : OrderPlaced, Uninitialized) => {
     goto (WaitingForAccountApproval) using SagaData(e.id, e.order, e.order.account, e.transactionId)
    }
  }

  when(WaitingForAccountApproval) {
    case Event( e : MoneyWithdrawalRequested, x: SagaData) =>
      goto (WaitingForOrderBookApproval)
  }

  when(WaitingForOrderBookApproval) {
    case Event( e : MoneyWithdrawalRequested, x: SagaData) =>
      goto (WaitingForAccountConfirmation)
  }

  when(WaitingForAccountConfirmation) {
    case Event( e : MoneyWithdrawalRequested, x: SagaData) =>
      goto (WaitingForOrderBookConfirmation)
  }

  when(WaitingForOrderBookConfirmation) {
    case Event( e : MoneyWithdrawalRequested, x: SagaData) =>
      goto (WaitingForAccountApproval)
  }

  onTransition {
    case Start -> WaitingForAccountApproval =>
      stateData match {
        case SagaData(orderBookId, order, accountId, transactionId) =>
          commandReceiver !  RequestMoneyWithdrawal(accountId, transactionId, order.amount )
     }
    case WaitingForAccountApproval -> WaitingForOrderBookApproval =>
      stateData match {
        case SagaData(orderBookId, order, accountId, transactionId) =>
          commandReceiver ! PrepareOrderPlacement(orderBookId, transactionId, OrderId("1"), order )
      }
    case WaitingForOrderBookApproval -> WaitingForAccountConfirmation =>
      stateData match {
        case SagaData(orderBookId, order, accountId, transactionId) =>
          commandReceiver ! ConfirmMoneyWithdrawal(accountId, transactionId)
      }
    case WaitingForAccountConfirmation -> WaitingForOrderBookConfirmation =>
      stop(Normal)
   }
}

class SagaRouter(val commandDispatcher: ActorRef) extends Actor with ActorLogging {
  var sagas = Map.empty[TransactionId, ActorRef]

  def receive = {

    case e : OrderPlaced => forward(e.transactionId, e)
    case e : OrderPlacementPrepared => forward(e.transactionId, e)
    case e : MoneyWithdrawalRequested  => forward(e.transactionId, e)
    case e : OrderPlacementConfirmed  => forward(e.transactionId, e)
    case e : MoneyWithdrawalConfirmed  => forward(e.transactionId, e)
  }
  def forward(transactionId : TransactionId, e: Event) {
    log.debug("Event received " + e)

    val saga = sagas.getOrElse(transactionId, context.system.actorOf(Props(new PlaceOrderSaga(commandDispatcher))))
    saga forward  e
  }

}
