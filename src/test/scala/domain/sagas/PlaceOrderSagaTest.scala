package domain.sagas

import org.scalatest.FunSuite
import akka.testkit.{TestKit, TestFSMRef, TestActorRef}
import akka.actor.{Props, ActorSystem, Actor}
import domain._
import domain.OrderBookId
import domain.OrderPlaced
import domain.LimitOrder
import domain.TransactionId
import org.joda.time.DateTime

/**
 * Created with IntelliJ IDEA.
 * User: guenter
 * Date: 06.07.13
 * Time: 10:59
 * To change this template use File | Settings | File Templates.
 */              /*
class CommandReceiverMock extends Actor {
  var receivedCommands = List.empty[Command]
  def receive = {
    case e: Command => receivedCommands = e :: receivedCommands
  }
}

class PlaceOrderSagaTest extends TestKit(ActorSystem("test")) with FunSuite {
  test("x") {
    val commandDispatcher = TestActorRef[CommandReceiverMock]
 //   val actorRef = system.actorOf(Props(new PlaceOrderSaga(commandDispatcher) ))
    val fsm = TestFSMRef(new PlaceOrderSaga(commandDispatcher))

    assert(fsm.stateName === Start)
    assert(fsm.stateData === Uninitialized)

    fsm ! OrderPlaced(OrderBookId("1"), TransactionId("2"), LimitOrder("1", new DateTime(), CurrencyUnit("BTC"), 5.0, Money(100, CurrencyUnit("EUR")), "B", AccountId("3"), AccountId("1")))

    assert(commandDispatcher.underlyingActor.receivedCommands.head === RequestMoneyWithdrawal(AccountId("3"), TransactionId("2"), Money(500, CurrencyUnit("EUR"))))
    fsm ! MoneyWithdrawalRequested(AccountId("3"), TransactionId("2"),Money(500, CurrencyUnit("EUR")), Money(0, CurrencyUnit("EUR")))

    assert(commandDispatcher.underlyingActor.receivedCommands.head === PrepareOrderPlacement(OrderBookId("1"), TransactionId("2"), OrderId("1"), LimitOrder("1", new DateTime(), CurrencyUnit("BTC"), 5.0, Money(100, CurrencyUnit("EUR")), "B", AccountId("1"), AccountId("3"))))

  }

}
    */