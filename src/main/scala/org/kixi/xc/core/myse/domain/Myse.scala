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

package org.kixi.xc.core.myse.domain

import org.kixi.cqrslib.aggregate.{AggregateRoot, Event, Identity, Command}
import org.joda.time.DateTime
import org.kixi.xc.core.orderbook.domain.Order
import java.util.UUID
import org.kixi.xc.core.account.domain.TransactionId

/**
 * Created by centos on 2/2/14.
 */
case class MyseId(id: String) extends Identity

sealed trait MyseCommand extends Command[MyseId]
sealed trait MyseEvent extends Event[MyseId]

case class PlaceOrder(id: MyseId, order: Order, timestamp: DateTime = new DateTime()) extends MyseCommand
case class OrderPlaced(id: MyseId, transactionId: TransactionId, order: Order, timestamp: DateTime = new DateTime()) extends MyseEvent {
  def hasSameContentAs[B <: Identity](other: Event[B]): Boolean = other == this.copy(timestamp = other.timestamp)
}

case class Myse(
  uncommittedEventsReverse: List[MyseEvent] = Nil,
  version: Int = 0,
  id: MyseId) extends AggregateRoot[Myse, MyseEvent, MyseId]{

  def loadedVersion(version: Int) = {
    copy(version = version)
  }

  def markCommitted = copy(uncommittedEventsReverse = Nil)

  def process(cmd: MyseCommand) = cmd match {
    case PlaceOrder(id, order, ts) =>
      applyEvent(OrderPlaced(id, TransactionId(), order))

  }

  override def applyEvent(e: MyseEvent): Myse = e match {
    case msg: OrderPlaced => when(msg)
  }

  def when(e: OrderPlaced): Myse = {
    copy(uncommittedEventsReverse = e :: uncommittedEventsReverse)
  }
}

