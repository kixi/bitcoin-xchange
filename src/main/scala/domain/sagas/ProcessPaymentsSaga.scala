package domain.sagas

import akka.actor._
import domain._
import akka.actor.FSM.Normal
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

/**
 * Created with IntelliJ IDEA.
 * User: guenter
 * Date: 03.07.13
 * Time: 06:41
 * To change this template use File | Settings | File Templates.
 */

class ProcessPaymentsSagaRouter(val commandDispatcher: ActorRef) extends Actor with ActorLogging {

  def receive = {

    case e : OrdersExecuted => {
      commandDispatcher ! DepositMoney(e.buy.productAccount, Money(e.buy.quantity, e.buy.product))
      commandDispatcher ! DepositMoney(e.sell.moneyAccount, e.price * e.sell.quantity)
    }
    case _ =>
  }

}
