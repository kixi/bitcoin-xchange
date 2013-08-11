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

package domain.sagas

import akka.actor._
import domain._
import akka.actor.FSM.Normal
import domain.RequestMoneyWithdrawal
import domain.OrderPlaced
import domain.LimitOrder
import domain.AccountId
import domain.PrepareOrderPlacement
import domain.OrderPlacementPrepared
import domain.ConfirmMoneyWithdrawal
import domain.OrderBookId
import domain.MoneyWithdrawalConfirmed
import domain.MoneyWithdrawalRequested
import domain.TransactionId
import domain.OrderPlacementConfirmed
import cqrs.{Identity, Event}

/**
 * Created with IntelliJ IDEA.
 * User: guenter
 * Date: 03.07.13
 * Time: 06:41
 * To change this template use File | Settings | File Templates.
 */
sealed trait State
case object Start extends State
case object End extends State
case object WaitingForAccountApproval extends State
case object WaitingForOrderBookApproval extends State
case object WaitingForAccountConfirmation extends State
case object WaitingForOrderBookConfirmation extends State

sealed trait Data
case object Uninitialized extends Data
case class SagaData(orderBookId: OrderBookId, order:LimitOrder, accountId: AccountId, transactionId: TransactionId ) extends Data

class PlaceOrderSaga(commandReceiver: ActorRef) extends Actor with LoggingFSM[State, Data] {
  startWith(Start, Uninitialized)

  when(Start) {
    case Event(e : OrderPlaced, Uninitialized) => {
     goto (WaitingForAccountApproval) using SagaData(e.id, e.order, if (e.order.buy) e.order.moneyAccount else e.order.productAccount, e.transactionId)
    }
  }

  when(WaitingForAccountApproval) {
    case Event( e : MoneyWithdrawalRequested, x: SagaData) =>
      goto (WaitingForOrderBookApproval)
  }

  when(WaitingForOrderBookApproval) {
    case Event( e : OrderPlacementPrepared, x: SagaData) =>
      goto (WaitingForAccountConfirmation)
  }

  when(WaitingForAccountConfirmation) {
    case Event( e : MoneyWithdrawalConfirmed, x: SagaData) =>
      goto (WaitingForOrderBookConfirmation)
  }

  when(WaitingForOrderBookConfirmation) {
    case Event( e : OrderPlacementConfirmed, x: SagaData) =>
      stop()
  }

  onTransition {
    case Start -> WaitingForAccountApproval =>
      nextStateData match {
        case SagaData(orderBookId, order, accountId, transactionId) =>
           commandReceiver !  RequestMoneyWithdrawal(accountId, transactionId, if (order.buy) order.amount else Money(order.quantity, order.product))
        case _ =>
     }
    case WaitingForAccountApproval -> WaitingForOrderBookApproval =>
      stateData match {
        case SagaData(orderBookId, order, accountId, transactionId) =>
          commandReceiver ! PrepareOrderPlacement(orderBookId, transactionId, order.id, order )
        case _ =>
      }
    case WaitingForOrderBookApproval -> WaitingForAccountConfirmation =>
      stateData match {
        case SagaData(orderBookId, order, accountId, transactionId) =>
          commandReceiver ! ConfirmMoneyWithdrawal(accountId, transactionId)
        case _ =>
      }
    case WaitingForAccountConfirmation -> WaitingForOrderBookConfirmation =>
      stateData match {
        case SagaData(orderBookId, order, accountId, transactionId) =>
          commandReceiver ! ConfirmOrderPlacement(orderBookId, transactionId, order.id, order)
        case _ =>
    }
    case WaitingForOrderBookConfirmation -> End => {
        stop()

    }

   }
}

class PlaceOrderSagaRouter(val commandDispatcher: ActorRef) extends Actor with ActorLogging {
  var sagas = Map.empty[TransactionId, ActorRef]

  def receive = {

    case e : OrderPlaced => forward(e.transactionId, e)
    case e : MoneyWithdrawalRequested  => forward(e.transactionId, e)
    case e : OrderPlacementPrepared => forward(e.transactionId, e)
    case e : MoneyWithdrawalConfirmed  => forward(e.transactionId, e)
    case e : OrderPlacementConfirmed  => {
      for(saga <- sagas.get(e.transactionId)) saga forward e
      sagas = sagas - e.transactionId
    }
    case _ =>
  }
  def forward(transactionId : TransactionId, e: Event[Identity]) {
 //   log.debug("Event received " + e)
    if (!sagas.contains(transactionId))
      sagas = sagas + (transactionId -> context.system.actorOf(Props(new PlaceOrderSaga(commandDispatcher))))
    for(saga <- sagas.get(transactionId)) saga forward e
  }

}
