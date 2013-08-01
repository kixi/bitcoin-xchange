package simulation

import akka.actor.{FSM, ActorRef, Actor}
import domain._
import domain.Money
import domain.OpenAccount
import domain.CurrencyUnit
import domain.AccountId
import java.util.UUID
import org.joda.time.DateTime
import scala.util.Random

/**
 * Created with IntelliJ IDEA.
 * User: guenter
 * Date: 27.07.13
 * Time: 23:13
 * To change this template use File | Settings | File Templates.
 */

sealed trait State

case object Start extends State

sealed trait Data

case object Uninitialized extends Data

case class InitialCapital(money: Money, bitcoins: Money) extends Data


case object StartSimulation

case object Deposit

class User(commandBus: ActorRef, userId: Int) extends Actor {

  val btcAcc = AccountId(userId.toString + "-BTC")
  val eurAcc = AccountId(userId.toString + "-EUR")

  def receive = {
    case StartSimulation =>
      commandBus ! OpenAccount(eurAcc, CurrencyUnit("EUR"))
      commandBus ! OpenAccount(btcAcc, CurrencyUnit("BTC"))
    case AccountOpened(accountId, _, _) if (accountId == eurAcc) =>
     commandBus ! DepositMoney(eurAcc, Money(10000, CurrencyUnit("EUR")))
    case AccountOpened(accountId, _, _) if (accountId == btcAcc) =>
      commandBus ! DepositMoney(btcAcc, Money(100, CurrencyUnit("BTC")))
    case MoneyDeposited(accountId, balance) if (accountId == btcAcc) =>
      val limit = new Random().nextDouble()
      val quantity = balance.amount
      commandBus ! PlaceOrder(OrderBookId("BTCEUR"), TransactionId(), LimitOrder(OrderId(),new DateTime(),CurrencyUnit("BTC"), quantity, Money(limit, CurrencyUnit("EUR")), Sell, eurAcc, btcAcc ))
    case MoneyDeposited(accountId, balance) if (accountId == btcAcc) =>
      val limit = new Random().nextDouble()
      val quantity = balance.amount / limit
      commandBus ! PlaceOrder(OrderBookId("BTCEUR"), TransactionId(), LimitOrder(OrderId(),new DateTime(),CurrencyUnit("BTC"), quantity, Money(limit, CurrencyUnit("EUR")), Buy, eurAcc, btcAcc ))
    case msg => System.out.println("Unexpected message: " + msg)
  }
}
