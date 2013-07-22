package domain

/**
 * Created with IntelliJ IDEA.
 * User: guenter
 * Date: 19.06.13
 * Time: 21:51
 * To change this template use File | Settings | File Templates.
 */

sealed trait Message extends Serializable
trait Command extends Message
trait Event extends Message
trait OrderBookCommand extends Command {
  def id: OrderBookId
}
trait OrderBookEvent extends Event {
  def id: OrderBookId
}

trait AccountCommand extends Command {
  def id: AccountId
}
trait AccountEvent extends Event {
  def id: AccountId
}

case class CreateOrderBook(id: OrderBookId) extends OrderBookCommand
case class OrderBookCreated(id: OrderBookId) extends OrderBookEvent

case class PlaceOrder(id: OrderBookId, transactionId: TransactionId, order: LimitOrder) extends OrderBookCommand
case class OrderPlaced(id: OrderBookId, transactionId: TransactionId, order: LimitOrder) extends OrderBookEvent

case class PrepareOrderPlacement(id: OrderBookId, transactionId: TransactionId, orderId: OrderId, order:LimitOrder) extends OrderBookCommand
case class OrderPlacementPrepared(id: OrderBookId, transactionId: TransactionId, orderId: OrderId, order:LimitOrder) extends OrderBookEvent

case class ConfirmOrderPlacement(id: OrderBookId, transactionId: TransactionId, orderId: OrderId, order:LimitOrder) extends OrderBookCommand
case class OrderPlacementConfirmed(id: OrderBookId, transactionId: TransactionId, orderId: OrderId) extends OrderBookEvent

case class OpenAccount(id: AccountId, currency: CurrencyUnit) extends AccountCommand
case class AccountOpened(id: AccountId, currency: CurrencyUnit, balance: Money) extends AccountEvent

case class DepositMoney(id: AccountId, amount: Money) extends AccountCommand
case class MoneyDeposited(id: AccountId, balance: Money) extends AccountEvent

case class RequestMoneyWithdrawal(id: AccountId, transactionId: TransactionId, amount: Money) extends AccountCommand
case class MoneyWithdrawalRequested(id: AccountId, transactionId: TransactionId, amount: Money, balance: Money) extends AccountEvent

case class ConfirmMoneyWithdrawal(id: AccountId, transactionId: TransactionId) extends AccountCommand
case class MoneyWithdrawalConfirmed(id: AccountId, transactionId: TransactionId) extends AccountEvent

