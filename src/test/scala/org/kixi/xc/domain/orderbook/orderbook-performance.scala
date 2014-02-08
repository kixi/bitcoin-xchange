import org.joda.time.DateTime
import org.kixi.xc.core.account.domain.AccountId
import org.kixi.xc.core.common.CurrencyUnit
import org.kixi.xc.core.common.Money
import org.kixi.xc.core.common.{Money, CurrencyUnit}
import org.kixi.xc.core.orderbook.domain._
import org.kixi.xc.core.orderbook.domain.LimitOrder
import org.kixi.xc.core.orderbook.domain.OrderBookId
import org.kixi.xc.core.orderbook.domain.OrderId
import org.scalatest.FunSuite
import scala.util.Random

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

/**
 * Created by centos on 1/21/14.
 */
class OrderBookPerf extends FunSuite {
  val now = new DateTime()

  test("performance") {
    var book = OrderBook(OrderBookId("EURBTC"), CurrencyUnit("EUR"))
    val p = Random.nextInt(100)+1
    val q = Random.nextInt(100)+1
    val buysell = if (Random.nextBoolean())
      Buy
    else
      Sell

    for (runs <- 0 to 10) {
      val start = System.nanoTime()
      for (i <- 0 to 10000)
        book = book.makeOrder(LimitOrder(OrderId(i.toString), now, CurrencyUnit("BTC"), q, Money(p, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC"))).markCommitted
      val end = System.nanoTime()
      println("10000 orders processed in " + 1.0e-6*(end - start))
    }


  }
}
