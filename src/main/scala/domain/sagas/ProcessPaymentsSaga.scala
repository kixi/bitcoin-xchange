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

/**
 * Created with IntelliJ IDEA.
 * User: guenter
 * Date: 03.07.13
 * Time: 06:41
 * To change this template use File | Settings | File Templates.
 */

class ProcessPaymentsSagaRouter(val commandDispatcher: ActorRef) extends Actor with ActorLogging {

  def receive = {

    case e : OrdersExecuted => {
      commandDispatcher ! DepositMoney(e.buy.productAccount, Money(e.buy.quantity, e.buy.product))
      commandDispatcher ! DepositMoney(e.sell.moneyAccount, e.price * e.sell.quantity)
    }
    case _ =>
  }

}
