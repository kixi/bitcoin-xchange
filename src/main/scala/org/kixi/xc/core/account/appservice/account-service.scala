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

package org.kixi.xc.core.account.appservice


import akka.actor.{Props, ActorLogging, Actor, ActorRef}
import scala.collection.immutable.Queue
import org.kixi.cqrslib.aggregate.Identity
import org.kixi.xc.core.account.domain._
import scala.Some
import org.kixi.xc.core.account.domain.Account
import org.kixi.xc.core.account.domain.AccountId
import org.kixi.myeventstore.{AppendEventsToStream, LoadEventStream, StreamRevision, EventsLoaded}

object AccountService {
  def props(bridge: Props) = Props(classOf[AccountService], bridge)
}

class AccountService(bridge: Props) extends Actor with ActorLogging {
  def receive = {
    case cmd: AccountCommand => {
      context.child("account-" + cmd.id.id) match {
        case Some(accountActor) => accountActor forward cmd
        case None => createActor(cmd.id) forward cmd
      }
    }
  }

  def createActor(accountId: AccountId) = {
    val actor = context.actorOf(AggregateActor.props(bridge, AccountState(accountId)), "account-" + accountId.id)
    context.watch(actor)
    actor
  }
}


trait ActorState {
  def aggregateId: String

  def aggregate: Option[Account]

  def restoreAggregate(msg: EventsLoaded[AccountCommand]): ActorState

  def process(cmd: AccountCommand): ActorState
}

case class AggregateNotLoadedException(id: Identity, aggregateType: Class[_], msg: String) extends RuntimeException(s"$aggregateType, id=$id: $msg")

case class AccountState(accountId: AccountId, aggregate: Option[Account] = None) extends ActorState {
  def aggregateId = accountId.id

  def restoreAggregate(msg: EventsLoaded[AccountCommand]): AccountState = {
    if (msg.stream.streamVersion != StreamRevision.Initial)
      copy(aggregate = Some(new AccountFactory().restoreFromHistory(msg.stream.events.asInstanceOf[List[AccountEvent]])))
    else
      copy(aggregate = None)
  }

  def process(cmd: AccountCommand): AccountState = cmd match {
    case cmd: OpenAccount =>
      copy(aggregate = Some(processCreate(cmd)))
    case cmd: AccountCommand =>
      aggregate match {
        case Some(account) =>
          copy(aggregate = Some(processAlter(cmd, account)))
        case None =>
          throw new AggregateNotLoadedException(accountId, Account.getClass, s"Unable to process command $cmd")
      }
  }

  def processCreate(cmd: AccountCommand): Account = cmd match {
    case cmd: OpenAccount =>
      new AccountFactory().create(cmd.id, cmd.currency)
  }

  def processAlter(cmd: AccountCommand, account: Account): Account = cmd match {
    case cmd: DepositMoney =>
      account.depositMoney(cmd.amount)
    case cmd: RequestMoneyWithdrawal =>
      account.requestMoneyWithdrawal(cmd.transactionId, cmd.amount)
    case cmd: ConfirmMoneyWithdrawal =>
      account.confirmMoneyWithdrawal(cmd.transactionId)
  }

  def publish(publishFunction: (String, List[AccountEvent]) => Unit): AccountState = {
    val events = aggregate.get.uncommittedEvents.reverse
    publishFunction(aggregateId, events)
    copy(aggregate = Some(aggregate.get.markCommitted))
  }

}

object AggregateActor {
  def props(bridge: Props, state: AccountState) = Props(classOf[AggregateActor], bridge, state)
}

class AggregateActor(bridge: Props, var state: AccountState) extends Actor with ActorLogging {

  val bridgeActor = context.actorOf(bridge, "bridge-" + state.aggregateId)

  override def preStart() = {
    log.info(s"restarting actore for $state")
    bridgeActor ! LoadEventStream(state.aggregateId, None)
  }

  def receive = loading()

  def loading(stash: Queue[(ActorRef, AccountCommand)] = Queue()): Receive = {
    case msg: EventsLoaded[AccountCommand] =>
      state = state.restoreAggregate(msg.asInstanceOf[EventsLoaded[AccountCommand]])
      log.debug(s"Aggregate restored $state")
      context become running
      processStash(stash)
    case command: AccountCommand =>
      log.debug(s"stashing while waiting for aggregate $state to be restored: $command")
      context become loading(stash enqueue (sender -> command))
    case msg =>
      log.warning(s"Unknown message $msg")
  }

  private def processStash(stash: Queue[(ActorRef, AccountCommand)]) {
    for ((s, cmd) <- stash) {
      self.tell(cmd, sender = s)
    }
  }

  def running: Receive = {
    case cmd: AccountCommand =>
      state = state.process(cmd.asInstanceOf[AccountCommand])
      state = state.publish(publish)
    case "committed" =>
    //TODO must be done by EventStore!
    // events.foreach(eventHandler ! _)
    case msg =>
      log.error(s"Unknown command $msg")
  }

  private def publish(streamId: String, events: List[AccountEvent]) {
    bridgeActor ! AppendEventsToStream(streamId, StreamRevision.NoConflict, events, None)
  }
}
