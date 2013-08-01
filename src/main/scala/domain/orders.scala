package domain

import org.joda.time.DateTime

/**
 * Created with IntelliJ IDEA.
 * User: guenter
 * Date: 19.06.13
 * Time: 22:06
 * To change this template use File | Settings | File Templates.
 */

trait Side

case object Buy extends Side

case object Sell extends Side

trait Order {
  def id: OrderId

  def product: CurrencyUnit

  def quantity: Quantity

  def creationDate: DateTime

  def side: Side

  def moneyAccount: AccountId

  def amount: Money

  def productAccount: AccountId

  def buy = (Buy == side)

  def sell = (Sell == side)

  def canExecute(order: Order): Boolean

  def hasHigherPriorityThan(order: Order): Boolean

  def expiryDate: DateTime = creationDate.plusDays(1)

  def expired = new DateTime().isAfter(expiryDate)
}

case class LimitOrder(
                       id: OrderId,
                       creationDate: DateTime = new DateTime,
                       product: CurrencyUnit,
                       quantity: Quantity,
                       limit: Money,
                       side: Side,
                       moneyAccount: AccountId,
                       productAccount: AccountId
                       ) extends Order {

  def canExecute(other: Order): Boolean = other match {
    case order: LimitOrder if this.buy && order.sell => this.limit >= order.limit
    case order: LimitOrder if this.sell && order.buy => this.limit <= order.limit
    case order: MarketOrder => true
    case _ => false
  }

  def hasHigherPriorityThan(other: Order): Boolean = {
    if (other.isInstanceOf[MarketOrder]) {
      this.creationDate.isBefore(other.creationDate)
    } else {
      val othLimit = other.asInstanceOf[LimitOrder]
      if (othLimit.buy && this.buy) {
        this.limit > othLimit.limit || (this.limit == othLimit.limit && this.creationDate.isBefore(othLimit.creationDate))
      } else if (othLimit.sell && this.sell) {
        this.limit < othLimit.limit || (this.limit == othLimit.limit && this.creationDate.isBefore(othLimit.creationDate))
      } else {
        false
      }
    }

  }

  def amount = limit * quantity
}

case class MarketOrder(
                        id: OrderId,
                        creationDate: DateTime = new DateTime,
                        product: CurrencyUnit,
                        quantity: Quantity,
                        price: Money,
                        side: Side,
                        moneyAccount: AccountId,
                        productAccount: AccountId
                        ) extends Order {

  def canExecute(other: Order): Boolean = {
    true
  }

  def hasHigherPriorityThan(other: Order): Boolean = {
    if (other.isInstanceOf[LimitOrder]) {
      true
    } else {
      this.creationDate.isBefore(other.creationDate)
    }
  }

  def amount = price * quantity
}

case class OrderBookList(buyOrders: List[Order], sellOrders: List[Order]) {
  def insert(order: Order) = {
    if (order.buy)
      copy(buyOrders=insertOrder(buyOrders, order))
    else
      copy(sellOrders=insertOrder(sellOrders, order))
  }

  def replaceHead(order: Order) = {
    if (order.buy)
      copy(buyOrders=order :: buyOrders.tail)
    else
      copy(sellOrders=order :: sellOrders.tail)
  }

  def removeHead(order: Order) = {
    if (order.buy)
      copy(buyOrders= buyOrders.tail)
    else
      copy(sellOrders= sellOrders.tail)
  }


  private def insertOrder(orderList: List[Order], order: Order): List[Order] = {
    if (orderList.isEmpty) order :: Nil
    else if (order.hasHigherPriorityThan(orderList.head)) {
      order :: orderList
    } else {
      orderList.head :: insertOrder(orderList.tail, order)
    }
  }

}
