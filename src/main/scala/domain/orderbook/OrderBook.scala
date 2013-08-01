package domain.orderbook

import domain._
import cqrs.{AggregateFactory, AggregateRoot}
import domain.UnhandledEventException
import domain.OrderBookCreated
import domain.OrderBookId

/**
 * Created with IntelliJ IDEA.
 * User: guenter
 * Date: 19.06.13
 * Time: 22:24
 * To change this template use File | Settings | File Templates.
 */

class OrderBookFactory extends AggregateFactory[OrderBook, OrderBookEvent] {

  def create(id: OrderBookId, currency: CurrencyUnit) = applyEvent(OrderBookCreated(id, currency))

  def applyEvent(e: OrderBookEvent) = e match {
    case e: OrderBookCreated => OrderBook(
      e :: Nil, 0, e.id,
      Map.empty[OrderId, LimitOrder], List.empty[Order], List.empty[Order],
      e.currency,
      Money(0,e.currency),
      Money(0,e.currency),
      Money(0,e.currency))
  }
}


case class OrderBook(
                      uncommittedEventsReverse: List[OrderBookEvent],
                      version: Int,
                      id: OrderBookId,
                      preparedOrders : Map[OrderId, LimitOrder],
                      buyOrders: List[Order],
                      sellOrders: List[Order],
                      currency: CurrencyUnit,
                      referencePrice: Money,
                      currentBid: Money,
                      currentAsk: Money)
  extends AggregateRoot[OrderBook, OrderBookEvent] {

  def placeOrder(transactionId: TransactionId, order: LimitOrder): OrderBook = {
    applyEvent(new OrderPlaced(id, transactionId, order))
  }

  def prepareOrderPlacement(orderId: OrderId, transactionId: TransactionId, order: LimitOrder) = {
    applyEvent(new OrderPlacementPrepared(id, transactionId, orderId, order))
  }

  def confirmOrderPlacement(orderId: OrderId, transactionId: TransactionId) = {
    preparedOrders.get(orderId) match {
      case Some(order:Order) => {
        applyEvent(new OrderPlacementConfirmed(id, transactionId,  orderId)).
          makeOrder(order)
      }
      case None => throw new RuntimeException("Order with ID "+orderId+" was not prepared and cannot be confirmed!")
    }
  }

  def cleanupFromExpiredOrders: OrderBook = {
    this
  }

  def makeOrder(order: Order): OrderBook = {
    if (order.quantity <= 0) {
      this
    } else if (order.expired) {
      throw new OrderExpiredException("Cannot make order! Order is already expired! " + order)
    } else {
      val cob = clearExpiredOrders(buyOrders).clearExpiredOrders(sellOrders)
      if (cob.canExecuteImmediately(order)) {
        cob.matchOrder(order, if (order.buy) cob.sellOrders else cob.buyOrders)
      } else {
        cob.queueOrder(order)
      }
    }
  }

  private def canExecuteImmediately(order: Order) = {
    if (order.buy) {
      !sellOrders.isEmpty && order.canExecute(sellOrders.head)
    } else {
      !buyOrders.isEmpty && order.canExecute(buyOrders.head)
    }
  }

  private def clearExpiredOrders(orders: List[Order]) : OrderBook = {
    if (!orders.isEmpty && orders.head.expired) {
      val this0 = applyEvent(OrderExpired(id, orders.head))
      this0.clearExpiredOrders(orders.tail)
    } else {
      this
    }
  }



  private def matchOrder(order: Order, matchList: List[Order]): OrderBook = {
    val queuedOrder = matchList.head
    val quantity = queuedOrder.quantity min order.quantity

    if (queuedOrder.quantity - quantity == 0) {
      val this1 = executeOrders(queuedOrder, copyOrder(order, quantity))
      val this2 = this1.makeOrder(copyOrder(order, order.quantity - quantity))
      this2
    } else {
      val this1 = executeOrders(copyOrder(queuedOrder, quantity), order)
      val this2 = this1.adjustOrder(copyOrder(queuedOrder, queuedOrder.quantity - order.quantity))
      this2
    }
  }

  private def copyOrder(order: Order, q: Quantity):Order = order match {
    case o: LimitOrder => o.copy(quantity = q)
    case o: MarketOrder => o.copy(quantity = q)
  }

  private def queueOrder(order: Order): OrderBook = {
      applyEvent(OrderQueued(this.id, order))
  }

  private def adjustOrder(order: Order): OrderBook = {
    applyEvent(OrderAdjusted(this.id, order))
  }

  private def executeOrders(order1: Order, order2: Order): OrderBook = {
    if (order1.buy) {
      applyEvent(OrdersExecuted(this.id, order1, order2, calculatePrice(order1, order2)))
    } else {
      applyEvent(OrdersExecuted(this.id, order2, order1, calculatePrice(order2, order1)))
    }
  }

  private def calculatePrice(buyOrder: Order, sellOrder: Order): Money = {
    buyOrder match {
      case b: LimitOrder =>
        sellOrder match {
          case s: LimitOrder => s.limit
          case s: MarketOrder => b.limit
        }
      case b: MarketOrder => {
        sellOrder match {
          case s: LimitOrder => s.limit
          case s: MarketOrder => referencePrice
        }
      }
    }

  }

  override def applyEvent(e: OrderBookEvent): OrderBook = e match {
    case e: OrderQueued => when(e)
     case e: OrderAdjusted => when(e)
    case e: OrdersExecuted => when(e)
    case e: OrderExpired => when(e)
    case event: OrderPlaced => when(event)
    case event: OrderPlacementPrepared => when(event)
    case event: OrderPlacementConfirmed => when(event)
    case event => throw new UnhandledEventException("Aggregate Account does not handle event " + event)
  }
  def markCommitted = copy(uncommittedEventsReverse = Nil)

  def when(event: OrderPlaced) = {
    copy(event :: uncommittedEventsReverse)
  }

  def when(event: OrderPlacementPrepared) = {
    copy(event :: uncommittedEventsReverse, preparedOrders = preparedOrders + (event.orderId -> event.order))
  }

  def when(event: OrderPlacementConfirmed) = {
    copy(event :: uncommittedEventsReverse)
  }


  def when(event: OrderQueued) = {
    if (event.order.buy)
      this.copy(uncommittedEventsReverse = event :: uncommittedEventsReverse, buyOrders = insertOrder(buyOrders, event.order))
    else
      this.copy(uncommittedEventsReverse = event :: uncommittedEventsReverse, sellOrders = insertOrder(sellOrders, event.order))
  }


  def when(event: OrderAdjusted) = {
    if (event.order.buy)
      this.copy(uncommittedEventsReverse = event :: uncommittedEventsReverse, buyOrders = event.order :: buyOrders.tail)
    else
      this.copy(uncommittedEventsReverse = event :: uncommittedEventsReverse, sellOrders = event.order :: sellOrders.tail)
  }

   def when(event: OrdersExecuted) = {
    if (!sellOrders.isEmpty && event.sell == sellOrders.head) {
      this.copy(uncommittedEventsReverse = event :: uncommittedEventsReverse, sellOrders = sellOrders.tail, referencePrice = event.price)
    } else if (!buyOrders.isEmpty && event.buy == buyOrders.head) {
      this.copy(uncommittedEventsReverse = event :: uncommittedEventsReverse, buyOrders = buyOrders.tail, referencePrice = event.price)
    } else {
      this.copy(uncommittedEventsReverse = event :: uncommittedEventsReverse, referencePrice = event.price)
    }
  }

  def when(event: OrderExpired) = {
    if (event.order.buy) {
      this.copy(uncommittedEventsReverse = event :: uncommittedEventsReverse, buyOrders = remove(buyOrders, event.order))
    } else {
      this.copy(uncommittedEventsReverse = event :: uncommittedEventsReverse, sellOrders = remove(sellOrders.tail, event.order))
    }
  }

  def remove(orderList: List[Order], order: Order): List[Order] = orderList match {
    case Nil => Nil
    case head :: tail =>
      if (head == order) tail
      else head :: remove(tail, order)
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

