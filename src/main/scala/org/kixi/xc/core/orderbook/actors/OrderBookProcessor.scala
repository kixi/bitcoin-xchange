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

package org.kixi.xc.core.orderbook.actors

import akka.persistence.{EventsourcedProcessor, Persistent, Processor}
import org.kixi.xc.core.orderbook.domain.{OrderBookEvent, OrderBookCommand, OrderBookId, OrderBook}
import org.kixi.xc.core.common.{CommandFailed, DomainException, CurrencyUnit}
import akka.actor.{ActorLogging, Actor, Props, ActorRef}

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

  private def createActor(orderbookId: OrderBookId) = {
    context.actorOf(OrderBookProcessor.props(orderbookId, eventPublisher), orderbookId.id)
  }
}

object OrderBookProcessor {
  def props(orderbookId: OrderBookId, eventPublisher: ActorRef) = Props(classOf[OrderBookProcessor], orderbookId, eventPublisher)
}

class OrderBookProcessor(orderbookId: OrderBookId, eventPublisher: ActorRef) extends EventsourcedProcessor with ActorLogging {

  var orderBook: OrderBook = OrderBook(orderbookId, CurrencyUnit("EUR"))

  override def processorId = "order-book:"+orderbookId.id

  def receiveRecover : Receive = {
    case evt: OrderBookEvent =>
      orderBook = orderBook.applyEvent(evt).markCommitted
  }

  def receiveCommand : Receive = {
    case msg: OrderBookCommand =>
      try {
        val orderBookUncommitted = orderBook.process(msg)
        persist(orderBookUncommitted.uncommittedEventsReverse.reverse) {
          event => eventPublisher ! event
        }
        orderBook = orderBookUncommitted.markCommitted
      } catch {
        case e: DomainException =>
          log.error(s"$processorId", e)
          eventPublisher ! CommandFailed(msg, processorId, e)
      }
  }
}


object OrderBookActor{
  def props(orderbookId: OrderBookId, eventPublisher: ActorRef) = Props(classOf[OrderBookActor], orderbookId, eventPublisher)
}
class OrderBookActor(orderbookId: OrderBookId, eventPublisher: ActorRef) extends Actor {

  var orderBook: OrderBook = OrderBook(orderbookId, CurrencyUnit("EUR"))

  def receive: Receive = {
    case msg: OrderBookCommand =>
      val orderBookUncommitted = orderBook.process(msg)
      orderBookUncommitted.uncommittedEventsReverse.reverse.foreach(
        event => eventPublisher ! event
      )
      orderBook = orderBookUncommitted.markCommitted
  }
}

