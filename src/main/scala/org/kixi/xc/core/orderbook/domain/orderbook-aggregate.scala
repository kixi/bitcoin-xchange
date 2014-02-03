/*
 * Copyright (c) 2013, Günter Kickinger.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * All advertising materials mentioning features or use of this software must
 * display the following acknowledgement: “This product includes software developed
 * by Günter Kickinger and his contributors.”
 * Neither the name of Günter Kickinger nor the names of its contributors may be
 * used to endorse or promote products derived from this software without specific
 * prior written permission.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS “AS IS”
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.kixi.xc.core.orderbook.domain

import org.kixi.cqrslib.aggregate.{AggregateRoot, AggregateFactory, Identity}
import org.kixi.xc.core.common._
import org.kixi.xc.core.common.UnhandledEventException
import org.kixi.xc.core.common.CurrencyUnit
import org.kixi.xc.core.common.OrderExpiredException
import scala.collection.immutable.TreeSet

case class OrderBookId(id: String) extends Identity

class OrderBookFactory extends AggregateFactory[OrderBook, OrderBookEvent, OrderBookId] {

  def create(id: OrderBookId, currency: CurrencyUnit) = applyEvent(OrderBookCreated(id, currency))

  def applyEvent(e: OrderBookEvent) = e match {
    case e: OrderBookCreated => OrderBook(
      id = e.id,
      currency = e.currency,
      referencePrice = Money(0, e.currency),
      currentBid = Money(0, e.currency),
      currentAsk = Money(0, e.currency))
  }
}
object OrderOrdering extends Ordering[Order] {
  def compare(o1: Order, o2: Order) = {
    if (o1.id == o2.id)
      0
    else {
      if (o1.hasHigherPriorityThan(o2)) {
        -1
      } else {
        1
      }
    }
  }
}
case class OrderBook(
                      uncommittedEventsReverse: List[OrderBookEvent] = Nil,
                      version: Int = 0,
                      id: OrderBookId,
                      buyOrders: TreeSet[Order] = TreeSet.empty(OrderOrdering),
                      sellOrders: TreeSet[Order] = TreeSet.empty(OrderOrdering),
                      currency: CurrencyUnit,
                      referencePrice: Money,
                      currentBid: Money,
                      currentAsk: Money)
  extends AggregateRoot[OrderBook, OrderBookEvent, OrderBookId] {

  def loadedVersion(version: Int) = {
    copy(version = version)
  }

  def process(cmd: OrderBookCommand): OrderBook = cmd match {
    case c: ProcessOrder =>
      val ob = makeOrder(c.order)
      ob.applyEvent(OrderProcessed(c.id, c.transactionId, c.order))
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

  private def clearExpiredOrders(orders: TreeSet[Order]): OrderBook = {
    var this0 = this
// to slow!!!
//    for (order <- orders if order.expired) {
//      this0 = this0.applyEvent(OrderExpired(id, order))
//    }
    this0
  }


  private def matchOrder(order: Order, matchList: TreeSet[Order]): OrderBook = {
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

  private def copyOrder(order: Order, q: Quantity): Order = order match {
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
    case event: OrderQueued => when(event)
    case event: OrderAdjusted => when(event)
    case event: OrdersExecuted => when(event)
    case event: OrderExpired => when(event)
    case event: OrderProcessed => when(event)
    case event => throw new UnhandledEventException("Aggregate OrderBook does not handle event " + event)
  }

  def markCommitted = copy(uncommittedEventsReverse = Nil)

  def when(event: OrderQueued) = {
    if (event.order.buy)
      this.copy(uncommittedEventsReverse = event :: uncommittedEventsReverse, buyOrders = insertOrder(buyOrders, event.order))
    else
      this.copy(uncommittedEventsReverse = event :: uncommittedEventsReverse, sellOrders = insertOrder(sellOrders, event.order))
  }


  def when(event: OrderAdjusted) = {
    if (event.order.buy)
      this.copy(uncommittedEventsReverse = event :: uncommittedEventsReverse, buyOrders = buyOrders + event.order)
    else
      this.copy(uncommittedEventsReverse = event :: uncommittedEventsReverse, sellOrders = sellOrders + event.order)
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

  def when(event: OrderProcessed) = {
    this.copy(uncommittedEventsReverse = event :: uncommittedEventsReverse)
  }

  def remove(orderList: TreeSet[Order], order: Order): TreeSet[Order] = {
    orderList.filter(o => o.id != order.id)
  }

  private def insertOrder(orderList: TreeSet[Order], order: Order): TreeSet[Order] = {

    orderList + order
  }

}

