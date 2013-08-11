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

package domain.account



import cqrs.{Identity, Event, AggregateProcessor}
import akka.actor.{Props, ActorLogging, Actor, ActorRef}
import domain._
import domain.RequestMoneyWithdrawal
import domain.OpenAccount
import domain.DepositMoney
import eventstore.EventStream
import domain.orderbook.{OrderBookFactory, OrderBook, OrderBookActor}

class AccountProcessor(eventStore: ActorRef) extends AggregateProcessor[AccountFactory,Account, AccountId, AccountCommand, AccountEvent](eventStore) {
  val factory = new AccountFactory

  def process(command: AccountCommand, stream: EventStream[Event[Identity]]) = command match {
    case cmd: OpenAccount => insert(stream, cmd, (Unit) => factory.create(cmd.id, cmd.currency))
    case cmd: DepositMoney => update(stream, cmd, (a:Account) => a.depositMoney(cmd.amount))
    case cmd: RequestMoneyWithdrawal => update(stream, cmd, (a:Account) => a.requestMoneyWithdrawal(cmd.transactionId, cmd.amount))
    case cmd: ConfirmMoneyWithdrawal => update(stream, cmd, (a:Account) => a.confirmMoneyWithdrawal(cmd.transactionId))
  }

}

object AccountService {
  def props(eventStore: ActorRef) = Props(classOf[AccountService], eventStore)
}
class AccountService(eventStore: ActorRef) extends Actor with ActorLogging {

  def receive = {
    case cmd: AccountCommand => {
      context.child(cmd.id.id) match {
        case Some(orderBookActor) => orderBookActor forward cmd
        case None => createActor(cmd.id) forward cmd
      }
    }
  }

  def createActor(accountId : AccountId) = {
    val actor = context.actorOf(AccountActor.props(eventStore, accountId),accountId.id)
    context.watch(actor)
    actor
  }

}

object AccountActor {
  def props(handler: ActorRef, accountId: AccountId) = Props(classOf[AccountActor], handler, accountId)
}
class AccountActor(eventHandler: ActorRef, accountId: AccountId) extends Actor with ActorLogging {

  var account : Account = _

  def receive = {
    case cmd: OpenAccount =>
      publishEvents(new AccountFactory().create(cmd.id, cmd.currency))
    case cmd: DepositMoney =>
      publishEvents(account.depositMoney(cmd.amount))
    case cmd: RequestMoneyWithdrawal =>
      publishEvents(account.requestMoneyWithdrawal(cmd.transactionId, cmd.amount))
    case cmd: ConfirmMoneyWithdrawal =>
      publishEvents(account.confirmMoneyWithdrawal(cmd.transactionId))
  }

  private def publishEvents(f: => Account) {
    account = f
    for (event <- account.uncommittedEventsReverse.reverse)
      eventHandler ! event
    account = account.markCommitted
  }

}
