package domain

/**
 * Created with IntelliJ IDEA.
 * User: guenter
 * Date: 19.06.13
 * Time: 21:51
 * To change this template use File | Settings | File Templates.
 */

trait Message
trait Command extends Message
trait Event
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

case class PlaceOrder(id: OrderBookId, order: OrderInfo) extends OrderBookCommand
case class OrderPlaced(id: OrderBookId, order: OrderInfo) extends OrderBookEvent

case class OpenAccount(id: AccountId) extends AccountCommand
case class AccountOpened(id: AccountId, balance: Balances) extends AccountEvent

case class DepositMoney(id: AccountId, amount: Money) extends AccountCommand
case class MoneyDeposited(id: AccountId, balance: Balances) extends AccountEvent

case class WithdrawMoney(id: AccountId, amount: Money) extends AccountCommand
case class MoneyWithdrawn(id: AccountId, balance: Balances) extends AccountEvent

