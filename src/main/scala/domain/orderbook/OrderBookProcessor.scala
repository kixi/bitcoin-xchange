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

import akka.actor._
import domain._
import cqrs.{Event, Identity, AggregateProcessor}
import scala.collection.immutable.Queue
import myeventstore._
import com.typesafe.config.Config
import akka.dispatch.{PriorityGenerator, UnboundedPriorityMailbox}
import domain.CreateOrderBook
import domain.ConfirmOrderPlacement
import scala.Some
import domain.PrepareOrderPlacement
import domain.OrderBookId
import domain.PlaceOrder
import domain.orderbook.OrderBook
import domain.CreateOrderBook
import myeventstore.LoadEventStream
import myeventstore.EventsConflicted
import domain.ConfirmOrderPlacement
import scala.Some
import myeventstore.EventStream
import myeventstore.EventsCommitted
import domain.PrepareOrderPlacement
import myeventstore.AppendEventsToStream
import myeventstore.EventsLoaded
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
      log.debug(s"Order book restored $orderBook")
      context become running
      processStash(stash)
     case command: OrderBookCommand =>
      log.debug(s"stashing while waiting for orderbook to be restored: $command")
      context become loading(stash enqueue (sender -> command))
    case msg =>
      log.warning(s"Unknown message $msg")
  }

  private def processStash(stash: Queue[(ActorRef, OrderBookCommand)]) {
    for((s, cmd) <- stash) {
      self.tell(cmd, sender = s )
    }
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
    case msg =>
      log.warning(s"Unknown message $msg")
  }

  def waiting4Confirmation(stash: Queue[(ActorRef, OrderBookCommand)] = Queue()) : Receive = {
    case command: OrderBookCommand =>
      val newqueue = stash enqueue (sender-> command)
    val size = newqueue.size
      log.debug(s"Command arrived while waiting for event store confirmation - stash size: $size command $command")
      context become waiting4Confirmation(newqueue)
    case EventsCommitted(commit) =>
      log.debug(s"Events committed: $commit")
      context become running
      processStash(stash)
    case EventsConflicted(conflict, backback) =>
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
    eventStore ! AppendEventsToStream(orderBookId.id, StreamRevision.NoConflict,orderBook.get.uncommittedEventsReverse.reverse, None )
  //  implicit val timeout = Timeout(5000)
  //  Await.result((eventStore ? AppendEventsToStream(orderBookId.toString, StreamRevision.NoConflict,orderBook.get.uncommittedEventsReverse.reverse, None )), timeout.duration)
    orderBook = Some(orderBook.get.markCommitted)
 //   context become waiting4Confirmation()
  }

}


class AggregateActorPrioMailbox[E](settings: ActorSystem.Settings, config: Config)
  extends UnboundedPriorityMailbox(
    // Create a new PriorityGenerator, lower prio means more important
    PriorityGenerator {
      // 'highpriority messages should be treated first if possible
      case msg: EventsCommitted[E] => 0

      case msg: EventsConflicted[E] => 0

      // PoisonPill when no other left
      case PoisonPill => 3

      // We default to 1, which is in between high and low
      case otherwise => 1
    })