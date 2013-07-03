package domain.orderbook

import domain._
import cqrs.{AggregateFactory, AggregateRoot}
import domain.UnhandledEventException
import domain.orderbook.OrderBook
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
    case e: OrderBookCreated => OrderBook(e :: Nil, 0, e.id)
  }
}

case class OrderBook(uncommittedEvents: List[OrderBookEvent], version: Int, id: OrderBookId) extends AggregateRoot[OrderBook, OrderBookEvent] {

  def placeOrder(order: OrderInfo): OrderBook = {
    applyEvent(new OrderPlaced(id, order))
  }

  def markCommitted = copy(uncommittedEvents = Nil)

  override def applyEvent(e: OrderBookEvent): OrderBook = {
    e match {
      case event: OrderPlaced => when(event)
       case event => throw new UnhandledEventException("Aggregate Account does not handle event " + event)
    }
  }

  def when(event: OrderPlaced) = {
    copy(event :: uncommittedEvents)
  }

}

