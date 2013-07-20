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

  def create(id: OrderBookId) = applyEvent(OrderBookCreated(id))

  def applyEvent(e: OrderBookEvent) = e match {
    case e: OrderBookCreated => OrderBook(e :: Nil, 0, e.id, Map.empty[OrderId, LimitOrder])
  }
}

case class OrderBook(uncommittedEvents: List[OrderBookEvent], version: Int, id: OrderBookId, preparedOrders : Map[OrderId, LimitOrder]) extends AggregateRoot[OrderBook, OrderBookEvent] {

  def placeOrder(transactionId: TransactionId, order: LimitOrder): OrderBook = {
    applyEvent(new OrderPlaced(id, transactionId, order))
  }

  def prepareOrderPlacement(orderId: OrderId, transactionId: TransactionId, order: LimitOrder) = {
    applyEvent(new OrderPlacementPrepared(id, transactionId, orderId, order))
  }

  def confirmOrderPlacement(orderId: OrderId, transactionId: TransactionId) = {
    applyEvent(new OrderPlacementConfirmed(id, transactionId,  orderId))
  }

  def markCommitted = copy(uncommittedEvents = Nil)

  override def applyEvent(e: OrderBookEvent): OrderBook = {
    e match {
      case event: OrderPlaced => when(event)
      case event: OrderPlacementPrepared => when(event)
      case event: OrderPlacementConfirmed => when(event)
       case event => throw new UnhandledEventException("Aggregate Account does not handle event " + event)
    }
  }

  def when(event: OrderPlaced) = {
    copy(event :: uncommittedEvents)
  }

  def when(event: OrderPlacementPrepared) = {
    copy(event :: uncommittedEvents)
  }

  def when(event: OrderPlacementConfirmed) = {
    copy(event :: uncommittedEvents)
  }

}

