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

package simulation

import akka.actor.{FSM, ActorRef, Actor}
import domain._
import domain.Money
import domain.OpenAccount
import domain.CurrencyUnit
import domain.AccountId
import java.util.UUID
import org.joda.time.DateTime
import scala.util.Random
import java.math.MathContext

/**
 * Created with IntelliJ IDEA.
 * User: guenter
 * Date: 27.07.13
 * Time: 23:13
 * To change this template use File | Settings | File Templates.
 */

sealed trait State

case object Start extends State

sealed trait Data

case object Uninitialized extends Data

case class InitialCapital(money: Money, bitcoins: Money) extends Data


case object StartSimulation

case object Deposit

class User(commandBus: ActorRef, userId: Int) extends Actor {

  val btcAcc = AccountId(userId.toString + "-BTC")
  val eurAcc = AccountId(userId.toString + "-EUR")

  def receive = {
    case StartSimulation =>
      commandBus ! OpenAccount(eurAcc, CurrencyUnit("EUR"))
      commandBus ! OpenAccount(btcAcc, CurrencyUnit("BTC"))
    case AccountOpened(accountId, _, _, _) if (accountId == eurAcc) =>
     commandBus ! DepositMoney(eurAcc, Money(10000, CurrencyUnit("EUR")))
    case AccountOpened(accountId, _, _, _) if (accountId == btcAcc) =>
      commandBus ! DepositMoney(btcAcc, Money(100, CurrencyUnit("BTC")))
    case MoneyDeposited(accountId, amount, _, _) if (accountId == btcAcc) =>
      val limit = 100.0 //+ new Random().nextDouble()
      val quantity = amount.amount
      if (quantity > 0)
       commandBus ! PlaceOrder(OrderBookId("BTCEUR"), TransactionId(), LimitOrder(OrderId(),new DateTime(),CurrencyUnit("BTC"), quantity, Money(BigDecimal(limit).setScale(2, BigDecimal.RoundingMode.DOWN), CurrencyUnit("EUR")), Sell, eurAcc, btcAcc ))
    case MoneyDeposited(accountId, amount, _, _) if (accountId == eurAcc) =>
      val limit = 100.0 //+ new Random().nextDouble()
      val quantity = ((amount.amount) / limit).setScale(0, BigDecimal.RoundingMode.HALF_DOWN)
      if (quantity > 0)
        commandBus ! PlaceOrder(OrderBookId("BTCEUR"), TransactionId(), LimitOrder(OrderId(),new DateTime(),CurrencyUnit("BTC"), quantity, Money(BigDecimal(limit).setScale(2, BigDecimal.RoundingMode.DOWN), CurrencyUnit("EUR")), Buy, eurAcc, btcAcc ))
    case msg => System.out.println("Unexpected message: " + msg)
  }
}
