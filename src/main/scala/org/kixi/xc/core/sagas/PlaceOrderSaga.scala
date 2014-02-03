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

package org.kixi.xc.core.sagas

import akka.actor._
import org.kixi.cqrslib.aggregate.{Identity, Event}
import org.kixi.xc.core.account.domain._
import org.kixi.xc.core.orderbook.domain._
import org.kixi.xc.core.common.Money
import org.kixi.xc.core.account.domain.MoneyWithdrawn
import org.kixi.xc.core.account.domain.WithdrawMoney
import org.kixi.xc.core.account.domain.AccountId
import org.kixi.xc.core.orderbook.domain.OrderBookId
import org.kixi.xc.core.myse.domain.OrderPlaced
import org.kixi.xc.core.sagas.PlaceOrderSaga._
import org.kixi.xc.core.myse.domain.OrderPlaced
import org.kixi.xc.core.common.Money
import org.kixi.xc.core.orderbook.domain.LimitOrder
import org.kixi.xc.core.account.domain.MoneyWithdrawn
import org.kixi.xc.core.account.domain.AccountId
import org.kixi.xc.core.orderbook.domain.OrderProcessed
import org.kixi.xc.core.orderbook.domain.OrderBookId
import org.kixi.xc.core.account.domain.TransactionId
import org.kixi.xc.core.account.domain.WithdrawMoney

object PlaceOrderSaga {
  sealed trait State
  case object Start extends State
  case object End extends State
  case object WaitingForWithdrawal extends State
  case object WaitingForOrderbook extends State
  sealed trait Data
  case object Uninitialized extends Data
  case class SagaData(orderBookId: OrderBookId, order: Order, accountId: AccountId, transactionId: TransactionId) extends Data
}

class PlaceOrderSaga(commandReceiver: ActorRef) extends Actor with LoggingFSM[State, Data] {
  startWith(Start, Uninitialized)

  when(Start) {
    case Event(e: OrderPlaced, Uninitialized) => {
      goto(WaitingForWithdrawal) using SagaData(OrderBookId("BTCEUR"), e.order, if (e.order.buy) e.order.moneyAccount else e.order.productAccount, e.transactionId)
    }
  }

  when(WaitingForWithdrawal) {
    case Event(e: MoneyWithdrawn, x: SagaData) =>
      goto(WaitingForOrderbook)
  }

  when(WaitingForOrderbook) {
    case Event(e: OrderProcessed, x: SagaData) =>
      stop()
  }

  onTransition {
    case Start -> WaitingForWithdrawal=>
      nextStateData match {
        case SagaData(orderBookId, order, accountId, transactionId) =>
          commandReceiver ! WithdrawMoney(accountId, transactionId, if (order.buy) order.amount else Money(order.quantity, order.product))
        case _ =>
      }
    case WaitingForWithdrawal -> WaitingForOrderbook =>
      stateData match {
        case SagaData(orderBookId, order, accountId, transactionId) =>
          commandReceiver ! ProcessOrder(orderBookId, transactionId, order)
        case _ =>
      }
    }
}

class PlaceOrderSagaRouter(val commandDispatcher: ActorRef) extends Actor with ActorLogging {
  var sagas = Map.empty[TransactionId, ActorRef]

  def receive = {

    case e: OrderPlaced => forward(e.transactionId, e)
    case e: MoneyWithdrawn => forward(e.transactionId, e)
    case e: OrderProcessed => {
      for (saga <- sagas.get(e.transactionId)) saga forward e
      sagas = sagas - e.transactionId
    }
    case _ =>
  }

  def forward(transactionId: TransactionId, e: Event[Identity]) {
    if (!sagas.contains(transactionId))
      sagas = sagas + (transactionId -> context.system.actorOf(Props(classOf[PlaceOrderSaga], commandDispatcher)))
    for (saga <- sagas.get(transactionId)) saga forward e
  }

}
