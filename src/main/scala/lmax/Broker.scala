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

package lmax

import scala.collection._
import domain._
import domain.account.{AccountFactory, Account}
import domain.orderbook.{OrderBookFactory, OrderBook}
import cqrs.{Identity, Command}
import domain.OrderBookId
import domain.OpenAccount
import domain.account.Account
import domain.DepositMoney
import domain.AccountId
import org.joda.time.DateTime

/**
 * Created with IntelliJ IDEA.
 * User: guenter
 * Date: 12.08.13
 * Time: 18:56
 * To change this template use File | Settings | File Templates.
 */
object Broker {
  val accounts = mutable.Map.empty[AccountId, Account]
  val orderBooks = mutable.Map.empty[OrderBookId, OrderBook]


  def process(cmd: AccountCommand) = cmd match {
    case OpenAccount(accountId, currency, _) =>
      accounts.put(accountId, new AccountFactory().create(accountId, currency))
    case DepositMoney(accountId, amount, _) =>
      for (acc <- accounts.get(accountId)) {
        accounts.put(accountId, acc.depositMoney(amount) )
      }

    case _=>
  }
  def process(cmd: OrderBookCommand) = cmd match {
    case CreateOrderBook(orderBookId, currency, _ ) =>
      orderBooks.put(orderBookId, new OrderBookFactory().create(orderBookId, currency))
    case PlaceOrder(orderBookId, transactionId, order, _) =>
      val Some(orderBook) = orderBooks.get(orderBookId)
      val Some(account) = if (order.buy) accounts.get(order.moneyAccount) else accounts.get(order.productAccount)
      accounts.put(account.id,
      if (order.buy) account.requestMoneyWithdrawal(transactionId, order.amount)
      else account.requestMoneyWithdrawal(transactionId, Money(order.quantity, account.currency)))

      orderBooks.put(orderBook.id, orderBook.makeOrder(order))
  }

}

object LMAX extends App {
  val start = System.currentTimeMillis
  Broker.process(CreateOrderBook(OrderBookId("BTCEUR"), CurrencyUnit("EUR")))
  for (count <- 0 to 300000) {
    Broker.process(OpenAccount(AccountId(s"$count-EUR"), CurrencyUnit("EUR")))
    Broker.process(DepositMoney(AccountId(s"$count-EUR"), Money(10000, CurrencyUnit("EUR"))))

    Broker.process(OpenAccount(AccountId(s"$count-BTC"), CurrencyUnit("BTC")))
    Broker.process(DepositMoney(AccountId(s"$count-BTC"), Money(100, CurrencyUnit("BTC"))))
   }

  for (count <- 0 to 300000) {
    Broker.process(PlaceOrder(OrderBookId("BTCEUR"), TransactionId(), LimitOrder(OrderId(), new DateTime(), CurrencyUnit("BTC"), 100, Money(100, CurrencyUnit("EUR")), Buy, AccountId(s"$count-EUR"), AccountId(s"$count-BTC") )))
    Broker.process(PlaceOrder(OrderBookId("BTCEUR"), TransactionId(), LimitOrder(OrderId(), new DateTime(), CurrencyUnit("BTC"), 100, Money(100, CurrencyUnit("EUR")), Sell, AccountId(s"$count-EUR"), AccountId(s"$count-BTC") )))
  }

  System.out.println("finsihed in " + (System.currentTimeMillis()-start)+ " ms")
}
