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
import scala.collection.immutable.Queue
import com.typesafe.config.Config
import akka.dispatch.{PriorityGenerator, UnboundedPriorityMailbox}
import org.kixi.xc.core.orderbook.domain._
import org.kixi.xc.core.orderbook.domain.CreateOrderBook
import scala.Some
import eventstore._
import akka.actor.Status.Failure
import org.kixi.xc.core.orderbook.domain.OrderBook
import eventstore.ReadStreamEventsCompleted
import org.kixi.xc.core.orderbook.domain.CreateOrderBook
import akka.actor.Status.Failure
import scala.Some
import eventstore.ReadStreamEvents
import eventstore.EsException
import org.kixi.xc.core.orderbook.domain.OrderBookId
import org.kixi.myeventstore.JavaSerializer
import java.util.UUID

object OrderBookService {
  def props(eventPublisher: ActorRef) = Props(classOf[OrderBookService], eventPublisher)
}

class OrderBookService(eventPublisher: ActorRef) extends Actor with ActorLogging {

  def receive = {
    case cmd: OrderBookCommand => {
      context.child(cmd.id.id) match {
        case Some(orderBookActor) => orderBookActor forward cmd
        case None => createActor(cmd.id) forward cmd
      }
    }
  }

  private def createActor(cmdId: OrderBookId) = {
    val actor = context.actorOf(OrderBookActor.props(eventPublisher, cmdId), cmdId.id)
    context.watch(actor)
    actor
  }
}

object OrderBookActor {
  def props(eventPublisher: ActorRef, orderBookId: OrderBookId) = Props(classOf[OrderBookActor], eventPublisher, orderBookId)
}

class OrderBookActor(eventPublisher: ActorRef, orderBookId: OrderBookId) extends Actor with ActorLogging {

  var aggregate: Option[OrderBook] = None

  override def preStart() = {
    log.debug("restoring aggregate")
    eventPublisher ! ReadStreamEvents(EventStream(aggregateId))
  }

  def receive = loading()

  def loading(stash: Queue[(ActorRef, Any)] = Queue()): Receive = {
    case msg: ReadStreamEventsCompleted =>
      aggregate = restoreAggregate(msg.events)
      log.debug(s"aggregate restored $aggregate")
      context become running
      processStash(stash)
    case Failure(e: EsException) =>
      log.error(s"aggregate not restored", e)
      context become running
      processStash(stash)
    case command =>
      log.debug(s"stashing while waiting for orderbook to be restored: $command")
      context become loading(stash enqueue (sender -> command))
  }

  private def processStash(stash: Queue[(ActorRef, Any)]) {
    for ((s, cmd) <- stash) {
      self.tell(cmd, s)
    }
  }

  private def restoreAggregate(events: List[Event]): Option[OrderBook] = events match {
    case Nil =>
      None
    case e =>
      Some(new OrderBookFactory().restoreFromHistory(
        (for (evt <- e) yield {
          val bytes = evt.data.data.value.toArray
          JavaSerializer.readObject(bytes).asInstanceOf[OrderBookEvent]
        }).toList))
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
  }

  def publishing(stash: Queue[(ActorRef, Any)] = Queue(), orderBook: OrderBook): Receive = {
    case msg: WriteEventsCompleted =>
      aggregate = Some(orderBook)
      context become running
      processStash(stash)
    case Failure(e: EsException) =>
      log.error("Error publishing events ", e)
      context become running
      processStash(stash)
    case msg =>
      context become publishing (stash enqueue (sender -> msg), orderBook)
  }

  private def publishEvents(f: OrderBook => OrderBook) {
    publish(f(aggregate.get))
  }

  private def publishEvents(f: => OrderBook) {
    publish(f)
  }

  private def publish(orderBook: OrderBook) {
    val events = {
      for (e <- aggregate.get.uncommittedEventsReverse.reverse)  yield {
        val bytes = JavaSerializer.writeObject(e)
        EventData("OrderBook", UUID.randomUUID(), Content(bytes))
      }
    }.toList
    eventPublisher ! WriteEvents(EventStream(aggregateId), events)
    context become(publishing(Queue(), orderBook.markCommitted))
  }
}
