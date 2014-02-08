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

package org.kixi.xc.singlethread

import scala.collection._
import org.joda.time.DateTime
import org.kixi.xc.core.orderbook.domain._
import org.kixi.xc.core.account.domain._
import org.kixi.xc.core.orderbook.domain.OrderBook
import org.kixi.xc.core.account.domain.Account
import org.kixi.xc.core.common.CurrencyUnit
import org.kixi.xc.core.account.domain.AccountId
import org.kixi.xc.core.common.Money
import org.kixi.xc.core.orderbook.domain.OrderBookId
import org.kixi.xc.core.account.domain.OpenAccount
import scala.Some
import org.kixi.xc.core.account.domain.DepositMoney
import org.kixi.xc.core.orderbook.domain.ProcessOrder

/**
 * User: guenter
 * Date: 12.08.13
 * Time: 18:56
 */
object Broker {
  val accounts = mutable.Map.empty[AccountId, Account]
  val orderBooks = mutable.Map.empty[OrderBookId, OrderBook]


  def process(cmd: AccountCommand) = cmd match {
    case OpenAccount(accountId, currency, _) =>
      accounts.put(accountId, new AccountFactory().create(accountId, currency))
    case DepositMoney(accountId, amount, _) =>
      for (acc <- accounts.get(accountId)) {
        accounts.put(accountId, acc.depositMoney(amount))
      }

    case _ =>
  }

  def process(cmd: OrderBookCommand) = cmd match {
    case ProcessOrder(orderBookId, transactionId, order, _) =>
      val Some(orderBook) = orderBooks.get(orderBookId)
      val Some(account) = if (order.buy) accounts.get(order.moneyAccount) else accounts.get(order.productAccount)
      accounts.put(account.id,
        if (order.buy) account.withdrawMoney(transactionId, order.amount)
        else account.withdrawMoney(transactionId, Money(order.quantity, account.currency)))

      orderBooks.put(orderBook.id, orderBook.makeOrder(order))
  }

}

object SingleThread extends App {
  val start = System.currentTimeMillis
  for (count <- 0 to 100000) {
    Broker.process(OpenAccount(AccountId(s"$count-EUR"), CurrencyUnit("EUR")))
    Broker.process(DepositMoney(AccountId(s"$count-EUR"), Money(10000, CurrencyUnit("EUR"))))

    Broker.process(OpenAccount(AccountId(s"$count-BTC"), CurrencyUnit("BTC")))
    Broker.process(DepositMoney(AccountId(s"$count-BTC"), Money(100, CurrencyUnit("BTC"))))
  }

  for (count <- 0 to 100000) {
    Broker.process(ProcessOrder(OrderBookId("BTCEUR"), TransactionId(), LimitOrder(OrderId(), new DateTime(), CurrencyUnit("BTC"), 100, Money(100, CurrencyUnit("EUR")), Buy, AccountId(s"$count-EUR"), AccountId(s"$count-BTC"))))
    Broker.process(ProcessOrder(OrderBookId("BTCEUR"), TransactionId(), LimitOrder(OrderId(), new DateTime(), CurrencyUnit("BTC"), 100, Money(100, CurrencyUnit("EUR")), Sell, AccountId(s"$count-EUR"), AccountId(s"$count-BTC"))))
  }

  System.out.println("finsihed in " + (System.currentTimeMillis() - start) + " ms")
}
