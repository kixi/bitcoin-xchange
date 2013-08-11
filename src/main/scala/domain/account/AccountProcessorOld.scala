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

package domain.account                         /*

import akka.actor.{ActorLogging, ActorRef, Actor}
import domain._
import domain.OpenAccount
import domain.DepositMoney
import domain.AccountId
import cqrs.{Identity, Event, EventStream}
import eventstore.{EventsCommitted, AppendEventsToStream, LoadEventStream}
import com.weiglewilczek.slf4s.Logger


class AccountProcessorOld(val eventStore: ActorRef) extends Actor with ActorLogging {
  val logger = Logger("ApplicationService")

  val factory = new AccountFactory()

  def receive = {
    case cmd: AccountCommand => eventStore ! LoadEventStream(cmd.id.id, cmd)
    case stream: EventStream => process(stream.boomerang.asInstanceOf[AccountCommand], stream)
    case commit: EventsCommitted =>
    case msg => logger.error("Unknown message " + msg)
  }

  def process(command: AccountCommand, stream: EventStream) = command match {
    case cmd: OpenAccount => insert(stream, cmd.id, (Unit) => factory.create(cmd.id, cmd.currency))
    case cmd: DepositMoney => update(stream, cmd.id, (a:Account) => a.depositMoney(cmd.amount))
    case cmd: RequestMoneyWithdrawal => update(stream, cmd.id, (a:Account) => a.requestMoneyWithdrawal(cmd.transactionId, cmd.amount))
    case cmd: ConfirmMoneyWithdrawal => update(stream, cmd.id, (a:Account) => a.confirmMoneyWithdrawal(cmd.transactionId))
  }

  def insert(stream: EventStream, id: AccountId, f: Unit => Account) {
    if (stream.streamVersion == 0) {
      val aggregate = f()
      eventStore ! AppendEventsToStream(id.id, aggregate.version, aggregate.uncommittedEvents.asInstanceOf[List[Event[Identity]]])
    } else
      throw new RuntimeException("account with this id already exists " + id)
  }

  def update(stream: EventStream, id: AccountId, f: Account => Account) {
    if (stream.streamVersion != 0) {
      val restoredAggr = factory.restoreFromHistory(stream.events.asInstanceOf[List[AccountEvent]])
      val newAggregate = f(restoredAggr)
      eventStore ! AppendEventsToStream(id.id, newAggregate.version, newAggregate.uncommittedEvents.asInstanceOf[List[Event[Identity]]])
    } else
      throw new RuntimeException("account with this id does not exist " + id)
  }




}
   */