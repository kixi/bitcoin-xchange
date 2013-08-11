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

package domain.orderbook

import akka.actor.{Props, ActorLogging, ActorRef, Actor}
import domain._
import domain.orderbook._
import cqrs.{Event, Identity, AggregateProcessor}
import eventstore._
import scala.collection._
import scala.collection.immutable.Queue
import org.omg.CORBA.portable.StreamableValue
import com.sun.corba.se.spi.ior.iiop.MaxStreamFormatVersionComponent
import domain.orderbook.OrderBook
import domain.CreateOrderBook
import eventstore.LoadEventStream
import domain.ConfirmOrderPlacement
import scala.Some
import eventstore.EventStream
import domain.PrepareOrderPlacement
import eventstore.EventsLoaded
import domain.OrderBookId
import domain.PlaceOrder

/**
 * Created with IntelliJ IDEA.
 * User: guenter
 * Date: 19.06.13
 * Time: 23:35
 * To change this template use File | Settings | File Templates.
 */
class OrderBookProcessor(val eventStore: ActorRef) extends AggregateProcessor[OrderBookFactory, OrderBook, OrderBookId, OrderBookCommand, OrderBookEvent](eventStore) {

  val factory = new OrderBookFactory()

  override def process(command: OrderBookCommand, stream: EventStream[Event[Identity]]): Unit = command match {
    case c: CreateOrderBook => insert(stream, c, (Unit) => factory.create(c.id, c.currency))
    case c: PlaceOrder => update(stream, c, _.placeOrder(c.transactionId, c.order))
    case c: PrepareOrderPlacement => update(stream, c, _.prepareOrderPlacement(c.orderId, c.transactionId, c.order))
    case c: ConfirmOrderPlacement => update(stream, c, _.confirmOrderPlacement(c.orderId, c.transactionId))
  }
}

object OrderBookService {
  def props(eventStore: ActorRef, handler: ActorRef) = Props(classOf[OrderBookService], eventStore, handler)
}

class OrderBookService(eventStore: ActorRef, handler: ActorRef) extends Actor with ActorLogging {

  def receive = {
    case cmd: OrderBookCommand => {
      context.child(cmd.id.id) match {
        case Some(orderBookActor) => orderBookActor forward cmd
        case None => createActor(cmd.id) forward cmd
      }
    }
  }

  private def createActor(cmdId : OrderBookId) = {
    val actor = context.actorOf(OrderBookActor.props(eventStore, handler, cmdId),cmdId.id)
    context.watch(actor)
    actor
  }
}

object OrderBookActor {
  def props(eventStore: ActorRef,handler: ActorRef, orderBookId: OrderBookId) = Props(classOf[OrderBookActor],eventStore, handler, orderBookId)
}

class OrderBookActor(eventStore: ActorRef, eventHandler: ActorRef, orderBookId: OrderBookId) extends Actor with ActorLogging {

  var orderBook : Option[OrderBook] = None

  override def preStart ={
    eventStore ! LoadEventStream(orderBookId.id, None)
  }

  def receive = loading()

  def loading(stash: Queue[(ActorRef, OrderBookCommand)] = Queue()) : Receive = {
    case msg: EventsLoaded[OrderBookCommand] =>
      orderBook = restoreAggregate(msg)
      context become running
      for((s, cmd) <- stash) {
        self.tell(cmd, sender = s )
      }
    case command: OrderBookCommand =>
      log.debug(s"stashing while waiting for orderbook to be restored: $command")
      context become loading(stash enqueue (sender -> command))
  }


  private def restoreAggregate(msg: EventsLoaded[OrderBookCommand]): Option[OrderBook] = {
    if (msg.stream.streamVersion != StreamRevision.Initial)
      Some(new OrderBookFactory().restoreFromHistory(msg.stream.events.asInstanceOf[List[OrderBookEvent]]))
    else
      None
  }

  def running : Receive = {
    case c: CreateOrderBook =>
      publishEvents(new OrderBookFactory().create(c.id, c.currency))
    case c: PlaceOrder =>
      publishEvents(_.placeOrder(c.transactionId, c.order))
    case c: PrepareOrderPlacement =>
      publishEvents(_.prepareOrderPlacement(c.orderId, c.transactionId, c.order))
    case c: ConfirmOrderPlacement =>
      publishEvents(_.confirmOrderPlacement(c.orderId, c.transactionId))
  }

  private def publishEvents(f: OrderBook => OrderBook) {
    orderBook = Some(f(orderBook.get))
    publish
  }

  private def publishEvents(f: => OrderBook) {
    orderBook = Some(f)
    publish
  }

  private def publish {
    for (event <- orderBook.get.uncommittedEventsReverse.reverse)
      eventHandler ! event
    orderBook = Some(orderBook.get.markCommitted)
  }

}
