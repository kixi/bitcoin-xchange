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

package org.kixi.xc.core.orderbook.appservice

import akka.actor._
import org.kixi.myeventstore
import myeventstore._
import scala.collection.immutable.Queue
import com.typesafe.config.Config
import akka.dispatch.{PriorityGenerator, UnboundedPriorityMailbox}
import myeventstore.LoadEventStream
import myeventstore.EventsConflicted
import myeventstore.EventsCommitted
import myeventstore.AppendEventsToStream
import myeventstore.EventsLoaded
import org.kixi.xc.core.orderbook.domain._
import org.kixi.xc.core.orderbook.domain.CreateOrderBook
import org.kixi.xc.core.orderbook.domain.ConfirmOrderPlacement
import scala.Some
import org.kixi.xc.core.orderbook.domain.PrepareOrderPlacement
import org.kixi.xc.core.orderbook.domain.PlaceOrder

object OrderBookService {
  def props(props: Props, handler: ActorRef) = Props(classOf[OrderBookService], props, handler)
}

class OrderBookService(props: Props, handler: ActorRef) extends Actor with ActorLogging {

  def receive = {
    case cmd: OrderBookCommand => {
      context.child(cmd.id.id) match {
        case Some(orderBookActor) => orderBookActor forward cmd
        case None => createActor(cmd.id) forward cmd
      }
    }
  }

  private def createActor(cmdId: OrderBookId) = {
    val actor = context.actorOf(OrderBookActor.props(props, handler, cmdId), cmdId.id)
    context.watch(actor)
    actor
  }
}

object OrderBookActor {
  def props(props: Props, handler: ActorRef, orderBookId: OrderBookId) = Props(classOf[OrderBookActor], props, handler, orderBookId)
}

class OrderBookActor(bridge: Props, eventHandler: ActorRef, orderBookId: OrderBookId) extends Actor with ActorLogging {

  var aggregate: Option[OrderBook] = None
  val bridgeActor = context.actorOf(bridge, "bridge-" + aggregateId)

  override def preStart() = {
    log.debug("restoring aggregate")
    bridgeActor ! LoadEventStream(aggregateId, None)
  }

  def receive = loading()

  def loading(stash: Queue[(ActorRef, Any)] = Queue()): Receive = {
    case msg: EventsLoaded[OrderBookEvent] =>
      aggregate = restoreAggregate(msg)
      log.debug(s"aggregate restored $aggregate")
      context become running
      processStash(stash)
    case command =>
      log.debug(s"stashing while waiting for orderbook to be restored: $command")
      context become loading(stash enqueue (sender -> command))
  }

  private def processStash(stash: Queue[(ActorRef, Any)]) {
    for ((s, cmd) <- stash) {
      self.tell(cmd, sender = s)
    }
  }

  private def restoreAggregate(msg: EventsLoaded[OrderBookEvent]): Option[OrderBook] = {
    if (msg.stream.streamVersion != StreamRevision.Initial)
      Some(new OrderBookFactory().restoreFromHistory(msg.stream.events.asInstanceOf[List[OrderBookEvent]]))
    else
      None
  }

  private def createAggregate(c: CreateOrderBook): OrderBook = {
    new OrderBookFactory().create(c.id, c.currency)
  }

  private def aggregateId = {
    orderBookId.id
  }
  
  def running: Receive = {
    case c: CreateOrderBook =>
      publishEvents(createAggregate(c))
    case msg: OrderBookCommand =>
      publishEvents(_.process(msg))
      log.warning(s"Unknown message $msg")
  }


  def waiting4Commit(stash: Queue[(ActorRef, Any)] = Queue()): Receive = {
    case EventsCommitted(commit) =>
      log.debug(s"Events committed: $commit")
      context become running
      processStash(stash)
    case EventsConflicted(conflict, backback) =>
    case command =>
      val newqueue = stash enqueue (sender -> command)
      val size = newqueue.size
      log.debug(s"Command arrived while waiting for event store confirmation - stash size: $size command $command")
      context become waiting4Commit(newqueue)
  }

  private def publishEvents(f: OrderBook => OrderBook) {
    aggregate = Some(f(aggregate.get))
    publish()
  }

  private def publishEvents(f: => OrderBook) {
    aggregate = Some(f)
    publish()
  }

  private def publish() {
    bridgeActor ! AppendEventsToStream(orderBookId.id, StreamRevision.NoConflict, aggregate.get.uncommittedEventsReverse.reverse, None)
    //  implicit val timeout = Timeout(5000)
    //  Await.result((eventStore ? AppendEventsToStream(orderBookId.toString, StreamRevision.NoConflict,orderBook.get.uncommittedEventsReverse.reverse, None )), timeout.duration)
    aggregate = Some(aggregate.get.markCommitted)
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