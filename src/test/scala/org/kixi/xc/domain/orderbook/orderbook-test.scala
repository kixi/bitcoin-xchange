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

package org.kixi.xc.domain.orderbook

import org.scalatest.FunSuite
import org.joda.time.DateTime
import org.kixi.xc.core.orderbook.domain._
import org.kixi.xc.core.common._
import org.kixi.xc.core.orderbook.domain.OrderAdjusted
import org.kixi.xc.core.orderbook.domain.OrderBook
import org.kixi.xc.core.orderbook.domain.OrderBookCreated
import org.kixi.xc.core.common.CurrencyUnit
import org.kixi.xc.core.common.OrderId
import org.kixi.xc.core.orderbook.domain.OrderPlaced
import org.kixi.xc.core.common.LimitOrder
import org.kixi.xc.core.orderbook.domain.OrderQueued
import org.kixi.xc.core.orderbook.domain.OrdersExecuted
import org.kixi.xc.core.common.Money
import org.kixi.xc.core.orderbook.domain.OrderPlacementPrepared
import org.kixi.xc.core.orderbook.domain.OrderBookId
import org.kixi.xc.core.account.domain.AccountId
import org.kixi.cqrslib.aggregate.SpecTest


class OrderBookTest extends SpecTest[OrderBook, OrderBookFactory, OrderBookEvent, OrderBookId] with FunSuite

class OrderBook_T1 extends OrderBookTest {
  val now = new DateTime()

  test("place buy limit order in empty orderbook") {
    given(new OrderBookFactory()) {
      OrderBookCreated(OrderBookId("BTCEUR"), CurrencyUnit("EUR")) ::
        Nil
    }

    when {
      ob: OrderBook => ob.makeOrder(LimitOrder(OrderId("1"), now, CurrencyUnit("BTC"), 5.0, Money(100, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC")))
    }

    expected {
      OrderQueued(OrderBookId("BTCEUR"), LimitOrder(OrderId("1"), now, CurrencyUnit("BTC"), 5.0, Money(100, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        Nil
    }
  }
}

class OrderBook_T2 extends OrderBookTest {
  val now = new DateTime()

  test("place sell limit order in empty orderbook") {
    given(new OrderBookFactory()) {
      OrderBookCreated(OrderBookId("BTCEUR"), CurrencyUnit("EUR")) ::
        Nil
    }

    when {
      ob: OrderBook => ob.makeOrder(LimitOrder(OrderId("1"), now, CurrencyUnit("BTC"), 5.0, Money(100, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC")))
    }

    expected {
      OrderQueued(OrderBookId("BTCEUR"), LimitOrder(OrderId("1"), now, CurrencyUnit("BTC"), 5.0, Money(100, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        Nil
    }
  }
}

class OrderBook_T3 extends OrderBookTest {
  val now = new DateTime()

  test("place buy market order in empty orderbook") {
    given(new OrderBookFactory()) {
      OrderBookCreated(OrderBookId("BTCEUR"), CurrencyUnit("EUR")) ::
        Nil
    }

    when {
      ob: OrderBook => ob.makeOrder(MarketOrder(OrderId("1"), now, CurrencyUnit("BTC"), 5.0, Money(100, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC")))
    }

    expected {
      OrderQueued(OrderBookId("BTCEUR"), MarketOrder(OrderId("1"), now, CurrencyUnit("BTC"), 5.0, Money(100, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        Nil
    }
  }
}

class OrderBook_T4 extends OrderBookTest {
  val now = new DateTime()

  test("place buy market order in empty orderbook") {
    given(new OrderBookFactory()) {
      OrderBookCreated(OrderBookId("BTCEUR"), CurrencyUnit("EUR")) ::
        Nil
    }

    when {
      ob: OrderBook => ob.makeOrder(MarketOrder(OrderId("1"), now, CurrencyUnit("BTC"), 5.0, Money(100, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC")))
    }

    expected {
      OrderQueued(OrderBookId("BTCEUR"), MarketOrder(OrderId("1"), now, CurrencyUnit("BTC"), 5.0, Money(100, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        Nil
    }
  }
}


class OrderBook_T5 extends OrderBookTest {
  val now = new DateTime()

  test("place second buy limit order in orderbook with 1 buy order") {
    given(new OrderBookFactory()) {
      OrderBookCreated(OrderBookId("BTCEUR"), CurrencyUnit("EUR")) ::
        OrderQueued(OrderBookId("BTCEUR"), LimitOrder(OrderId("1"), now, CurrencyUnit("BTC"), 5.0, Money(100, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        Nil
    }

    when {
      ob: OrderBook => ob.makeOrder(LimitOrder(OrderId("1"), now, CurrencyUnit("BTC"), 5.0, Money(105, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC")))
    }

    expected {
      OrderQueued(OrderBookId("BTCEUR"), LimitOrder(OrderId("1"), now, CurrencyUnit("BTC"), 5.0, Money(105, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        Nil
    }
  }
}

class OrderBook_T6 extends OrderBookTest {
  val now = new DateTime()

  test("place second sell limit order in orderbook with 1 sell order") {
    given(new OrderBookFactory()) {
      OrderBookCreated(OrderBookId("BTCEUR"), CurrencyUnit("EUR")) ::
        OrderQueued(OrderBookId("BTCEUR"), LimitOrder(OrderId("1"), now, CurrencyUnit("BTC"), 5.0, Money(100, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        Nil
    }

    when {
      ob: OrderBook => ob.makeOrder(LimitOrder(OrderId("1"), now, CurrencyUnit("BTC"), 5.0, Money(105, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC")))
    }

    expected {
      OrderQueued(OrderBookId("BTCEUR"), LimitOrder(OrderId("1"), now, CurrencyUnit("BTC"), 5.0, Money(105, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        Nil
    }
  }
}

class OrderBook_T7_1 extends OrderBookTest {
  val now = new DateTime()

  test("buy order for existing sell order which match with quantity and amount") {
    given(new OrderBookFactory()) {
      OrderBookCreated(OrderBookId("BTCEUR"), CurrencyUnit("EUR")) ::
        OrderQueued(OrderBookId("BTCEUR"), LimitOrder(OrderId("1"), now, CurrencyUnit("BTC"), 5.0, Money(100, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        Nil
    }

    when {
      ob: OrderBook => ob.makeOrder(LimitOrder(OrderId("1"), now, CurrencyUnit("BTC"), 5.0, Money(100, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC")))
    }

    expected {
      OrdersExecuted(OrderBookId("BTCEUR"),
        LimitOrder(OrderId("1"), now, CurrencyUnit("BTC"), 5.0, Money(100, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC")),
        LimitOrder(OrderId("1"), now, CurrencyUnit("BTC"), 5.0, Money(100, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC")),
        Money(100, CurrencyUnit("EUR"))
      ) ::
        Nil
    }
  }
}

class OrderBook_T7_2 extends OrderBookTest {
  val now = new DateTime()

  test("buy order for existing sell order which match with quantity and amount - sell order is no longer in orderbook") {
    given(new OrderBookFactory()) {
      OrderBookCreated(OrderBookId("BTCEUR"), CurrencyUnit("EUR")) ::
        OrderQueued(OrderBookId("BTCEUR"), LimitOrder(OrderId("1"), now, CurrencyUnit("BTC"), 5.0, Money(100, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        Nil
    }

    when {
      ob: OrderBook => {
        ob.makeOrder(LimitOrder(OrderId("1"), now, CurrencyUnit("BTC"), 5.0, Money(100, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC"))).
          markCommitted.
          makeOrder(LimitOrder(OrderId("1"), now, CurrencyUnit("BTC"), 5.0, Money(100, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC")))
      }
    }

    expected {
      OrderQueued(OrderBookId("BTCEUR"),
        LimitOrder(OrderId("1"), now, CurrencyUnit("BTC"), 5.0, Money(100, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC"))
      ) ::
        Nil
    }
  }
}

class OrderBook_T7_3 extends OrderBookTest {
  val now = new DateTime()

  test("buy order for existing sell order which match with quantity and amount - buy order is no longer in orderbook") {
    given(new OrderBookFactory()) {
      OrderBookCreated(OrderBookId("BTCEUR"), CurrencyUnit("EUR")) ::
        OrderQueued(OrderBookId("BTCEUR"), LimitOrder(OrderId("1"), now, CurrencyUnit("BTC"), 5.0, Money(100, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        Nil
    }

    when {
      ob: OrderBook => {
        ob.makeOrder(LimitOrder(OrderId("1"), now, CurrencyUnit("BTC"), 5.0, Money(100, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC"))).
          markCommitted.
          makeOrder(LimitOrder(OrderId("1"), now, CurrencyUnit("BTC"), 5.0, Money(100, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC")))
      }
    }

    expected {
      OrderQueued(OrderBookId("BTCEUR"),
        LimitOrder(OrderId("1"), now, CurrencyUnit("BTC"), 5.0, Money(100, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC"))
      ) ::
        Nil
    }
  }
}


class OrderBook_T8_1 extends OrderBookTest {
  val now = new DateTime()

  test("buy order for existing sell order which match with quantity and amount") {
    given(new OrderBookFactory()) {
      OrderBookCreated(OrderBookId("BTCEUR"), CurrencyUnit("EUR")) ::
        OrderQueued(OrderBookId("BTCEUR"), LimitOrder(OrderId("1"), now, CurrencyUnit("BTC"), 5.0, Money(100, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        Nil
    }

    when {
      ob: OrderBook => ob.makeOrder(LimitOrder(OrderId("1"), now, CurrencyUnit("BTC"), 5.0, Money(105, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC")))
    }

    expected {
      OrdersExecuted(OrderBookId("BTCEUR"),
        LimitOrder(OrderId("1"), now, CurrencyUnit("BTC"), 5.0, Money(105, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC")),
        LimitOrder(OrderId("1"), now, CurrencyUnit("BTC"), 5.0, Money(100, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC")),
        Money(100, CurrencyUnit("EUR"))
      ) ::
        Nil
    }
  }
}

class OrderBook_T8_2 extends OrderBookTest {
  val now = new DateTime()

  test("buy order for existing sell order which match with quantity and amount - sell order is no longer in orderbook") {
    given(new OrderBookFactory()) {
      OrderBookCreated(OrderBookId("BTCEUR"), CurrencyUnit("EUR")) ::
        OrderQueued(OrderBookId("BTCEUR"), LimitOrder(OrderId("1"), now, CurrencyUnit("BTC"), 5.0, Money(100, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        Nil
    }

    when {
      ob: OrderBook => {
        ob.makeOrder(LimitOrder(OrderId("1"), now, CurrencyUnit("BTC"), 5.0, Money(105, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC"))).
          markCommitted.
          makeOrder(LimitOrder(OrderId("1"), now, CurrencyUnit("BTC"), 5.0, Money(105, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC")))
      }
    }

    expected {
      OrderQueued(OrderBookId("BTCEUR"),
        LimitOrder(OrderId("1"), now, CurrencyUnit("BTC"), 5.0, Money(105, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC"))
      ) ::
        Nil
    }
  }
}

class OrderBook_T8_3 extends OrderBookTest {
  val now = new DateTime()

  test("buy order for existing sell order which match with quantity and amount - buy order is no longer in orderbook") {
    given(new OrderBookFactory()) {
      OrderBookCreated(OrderBookId("BTCEUR"), CurrencyUnit("EUR")) ::
        OrderQueued(OrderBookId("BTCEUR"), LimitOrder(OrderId("1"), now, CurrencyUnit("BTC"), 5.0, Money(100, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        Nil
    }

    when {
      ob: OrderBook => {
        ob.makeOrder(LimitOrder(OrderId("1"), now, CurrencyUnit("BTC"), 5.0, Money(105, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC"))).
          markCommitted.
          makeOrder(LimitOrder(OrderId("1"), now, CurrencyUnit("BTC"), 5.0, Money(100, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC")))
      }
    }

    expected {
      OrderQueued(OrderBookId("BTCEUR"),
        LimitOrder(OrderId("1"), now, CurrencyUnit("BTC"), 5.0, Money(100, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC"))
      ) ::
        Nil
    }
  }
}

class OrderBook_T9_1 extends OrderBookTest {
  val now = new DateTime()

  test("buy order for existing sell order which match with quantity and amount") {
    given(new OrderBookFactory()) {
      OrderBookCreated(OrderBookId("BTCEUR"), CurrencyUnit("EUR")) ::
        OrderQueued(OrderBookId("BTCEUR"), LimitOrder(OrderId("1"), now, CurrencyUnit("BTC"), 5.0, Money(100, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        Nil
    }

    when {
      ob: OrderBook => ob.makeOrder(LimitOrder(OrderId("1"), now, CurrencyUnit("BTC"), 5.0, Money(100, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC")))
    }

    expected {
      OrdersExecuted(OrderBookId("BTCEUR"),
        LimitOrder(OrderId("1"), now, CurrencyUnit("BTC"), 5.0, Money(100, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC")),
        LimitOrder(OrderId("1"), now, CurrencyUnit("BTC"), 5.0, Money(100, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC")),
        Money(100, CurrencyUnit("EUR"))
      ) ::
        Nil
    }
  }
}

class OrderBook_T9_2 extends OrderBookTest {
  val now = new DateTime()

  test("buy order for existing sell order which match with quantity and amount") {
    given(new OrderBookFactory()) {
      OrderBookCreated(OrderBookId("BTCEUR"), CurrencyUnit("EUR")) ::
        OrderQueued(OrderBookId("BTCEUR"), LimitOrder(OrderId("1"), now, CurrencyUnit("BTC"), 5.0, Money(100, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        OrdersExecuted(OrderBookId("BTCEUR"),
          LimitOrder(OrderId("1"), now, CurrencyUnit("BTC"), 5.0, Money(100, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC")),
          LimitOrder(OrderId("1"), now, CurrencyUnit("BTC"), 5.0, Money(100, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC")),
          Money(100, CurrencyUnit("EUR"))
        ) :: Nil
    }

    when {
      ob: OrderBook => ob.makeOrder(LimitOrder(OrderId("1"), now, CurrencyUnit("BTC"), 5.0, Money(100, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC")))
    }

    expected {
      OrderQueued(OrderBookId("BTCEUR"),
        LimitOrder(OrderId("1"), now, CurrencyUnit("BTC"), 5.0, Money(100, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC"))
      ) ::
        Nil
    }
  }
}

class OrderBook_T9_3 extends OrderBookTest {
  val now = new DateTime()

  test("buy order for existing sell order which match with quantity and amount") {
    given(new OrderBookFactory()) {
      OrderBookCreated(OrderBookId("BTCEUR"), CurrencyUnit("EUR")) ::
        OrderQueued(OrderBookId("BTCEUR"), LimitOrder(OrderId("1"), now, CurrencyUnit("BTC"), 5.0, Money(100, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        OrdersExecuted(OrderBookId("BTCEUR"),
          LimitOrder(OrderId("1"), now, CurrencyUnit("BTC"), 5.0, Money(100, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC")),
          LimitOrder(OrderId("1"), now, CurrencyUnit("BTC"), 5.0, Money(100, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC")),
          Money(100, CurrencyUnit("EUR"))
        ) :: Nil
    }

    when {
      ob: OrderBook => ob.makeOrder(LimitOrder(OrderId("1"), now, CurrencyUnit("BTC"), 5.0, Money(100, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC")))
    }

    expected {
      OrderQueued(OrderBookId("BTCEUR"),
        LimitOrder(OrderId("1"), now, CurrencyUnit("BTC"), 5.0, Money(100, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC"))
      ) ::
        Nil
    }
  }
}


class OrderBook_T10 extends OrderBookTest {
  val now = new DateTime()

  test("take highest bid for limit orders") {
    given(new OrderBookFactory()) {
      OrderBookCreated(OrderBookId("BTCEUR"), CurrencyUnit("EUR")) ::
        OrderQueued(OrderBookId("BTCEUR"), LimitOrder(OrderId("1"), now, CurrencyUnit("BTC"), 5.0, Money(100, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        OrderQueued(OrderBookId("BTCEUR"), LimitOrder(OrderId("2"), now, CurrencyUnit("BTC"), 5.0, Money(110, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        OrderQueued(OrderBookId("BTCEUR"), LimitOrder(OrderId("3"), now, CurrencyUnit("BTC"), 5.0, Money(111, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        OrderQueued(OrderBookId("BTCEUR"), LimitOrder(OrderId("4"), now, CurrencyUnit("BTC"), 5.0, Money(67, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        OrderQueued(OrderBookId("BTCEUR"), LimitOrder(OrderId("5"), now, CurrencyUnit("BTC"), 5.0, Money(98, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        OrderQueued(OrderBookId("BTCEUR"), LimitOrder(OrderId("6"), now, CurrencyUnit("BTC"), 5.0, Money(100, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        OrderQueued(OrderBookId("BTCEUR"), LimitOrder(OrderId("7"), now, CurrencyUnit("BTC"), 5.0, Money(106, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        OrderQueued(OrderBookId("BTCEUR"), LimitOrder(OrderId("8"), now, CurrencyUnit("BTC"), 5.0, Money(105, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        Nil
    }

    when {
      ob: OrderBook => ob.makeOrder(LimitOrder(OrderId("10"), now, CurrencyUnit("BTC"), 5.0, Money(50, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC")))
    }

    expected {
      OrdersExecuted(OrderBookId("BTCEUR"),
        LimitOrder(OrderId("3"), now, CurrencyUnit("BTC"), 5.0, Money(111, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC")),
        LimitOrder(OrderId("10"), now, CurrencyUnit("BTC"), 5.0, Money(50, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC")),
        Money(50, CurrencyUnit("EUR"))
      ) ::
        Nil
    }
  }
}

class OrderBook_T11 extends OrderBookTest {
  val now = new DateTime()

  test("take lowest ask for limit orders") {
    given(new OrderBookFactory()) {
      OrderBookCreated(OrderBookId("BTCEUR"), CurrencyUnit("EUR")) ::
        OrderQueued(OrderBookId("BTCEUR"), LimitOrder(OrderId("1"), now, CurrencyUnit("BTC"), 5.0, Money(100, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        OrderQueued(OrderBookId("BTCEUR"), LimitOrder(OrderId("2"), now, CurrencyUnit("BTC"), 5.0, Money(110, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        OrderQueued(OrderBookId("BTCEUR"), LimitOrder(OrderId("3"), now, CurrencyUnit("BTC"), 5.0, Money(111, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        OrderQueued(OrderBookId("BTCEUR"), LimitOrder(OrderId("4"), now, CurrencyUnit("BTC"), 5.0, Money(67, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        OrderQueued(OrderBookId("BTCEUR"), LimitOrder(OrderId("5"), now, CurrencyUnit("BTC"), 5.0, Money(98, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        OrderQueued(OrderBookId("BTCEUR"), LimitOrder(OrderId("6"), now, CurrencyUnit("BTC"), 5.0, Money(100, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        OrderQueued(OrderBookId("BTCEUR"), LimitOrder(OrderId("7"), now, CurrencyUnit("BTC"), 5.0, Money(106, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        OrderQueued(OrderBookId("BTCEUR"), LimitOrder(OrderId("8"), now, CurrencyUnit("BTC"), 5.0, Money(105, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        Nil
    }

    when {
      ob: OrderBook => ob.makeOrder(LimitOrder(OrderId("10"), now, CurrencyUnit("BTC"), 5.0, Money(120, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC")))
    }

    expected {
      OrdersExecuted(OrderBookId("BTCEUR"),
        LimitOrder(OrderId("10"), now, CurrencyUnit("BTC"), 5.0, Money(120, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC")),
        LimitOrder(OrderId("4"), now, CurrencyUnit("BTC"), 5.0, Money(67, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC")),
        Money(67, CurrencyUnit("EUR"))
      ) ::
        Nil
    }
  }
}

class OrderBook_T12 extends OrderBookTest {
  val now = new DateTime()

  test("take market order over limit orders for bids") {
    given(new OrderBookFactory()) {
      OrderBookCreated(OrderBookId("BTCEUR"), CurrencyUnit("EUR")) ::
        OrderQueued(OrderBookId("BTCEUR"), LimitOrder(OrderId("1"), now, CurrencyUnit("BTC"), 5.0, Money(100, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        OrderQueued(OrderBookId("BTCEUR"), LimitOrder(OrderId("2"), now, CurrencyUnit("BTC"), 5.0, Money(110, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        OrderQueued(OrderBookId("BTCEUR"), LimitOrder(OrderId("3"), now, CurrencyUnit("BTC"), 5.0, Money(111, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        OrderQueued(OrderBookId("BTCEUR"), MarketOrder(OrderId("4"), now, CurrencyUnit("BTC"), 5.0, Money(101, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        OrderQueued(OrderBookId("BTCEUR"), LimitOrder(OrderId("5"), now, CurrencyUnit("BTC"), 5.0, Money(98, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        OrderQueued(OrderBookId("BTCEUR"), LimitOrder(OrderId("6"), now, CurrencyUnit("BTC"), 5.0, Money(100, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        OrderQueued(OrderBookId("BTCEUR"), LimitOrder(OrderId("7"), now, CurrencyUnit("BTC"), 5.0, Money(106, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        OrderQueued(OrderBookId("BTCEUR"), LimitOrder(OrderId("8"), now, CurrencyUnit("BTC"), 5.0, Money(105, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        Nil
    }

    when {
      ob: OrderBook => ob.makeOrder(LimitOrder(OrderId("10"), now, CurrencyUnit("BTC"), 5.0, Money(500, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC")))
    }

    expected {
      OrdersExecuted(OrderBookId("BTCEUR"),
        MarketOrder(OrderId("4"), now, CurrencyUnit("BTC"), 5.0, Money(101, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC")),
        LimitOrder(OrderId("10"), now, CurrencyUnit("BTC"), 5.0, Money(500, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC")),
        Money(500, CurrencyUnit("EUR"))
      ) ::
        Nil
    }
  }
}

class OrderBook_T13 extends OrderBookTest {
  val now = new DateTime()

  test("take market order over limit orders for asks") {
    given(new OrderBookFactory()) {
      OrderBookCreated(OrderBookId("BTCEUR"), CurrencyUnit("EUR")) ::
        OrderQueued(OrderBookId("BTCEUR"), LimitOrder(OrderId("1"), now, CurrencyUnit("BTC"), 5.0, Money(100, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        OrderQueued(OrderBookId("BTCEUR"), LimitOrder(OrderId("2"), now, CurrencyUnit("BTC"), 5.0, Money(110, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        OrderQueued(OrderBookId("BTCEUR"), LimitOrder(OrderId("3"), now, CurrencyUnit("BTC"), 5.0, Money(111, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        OrderQueued(OrderBookId("BTCEUR"), MarketOrder(OrderId("4"), now, CurrencyUnit("BTC"), 5.0, Money(101, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        OrderQueued(OrderBookId("BTCEUR"), LimitOrder(OrderId("5"), now, CurrencyUnit("BTC"), 5.0, Money(98, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        OrderQueued(OrderBookId("BTCEUR"), LimitOrder(OrderId("6"), now, CurrencyUnit("BTC"), 5.0, Money(100, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        OrderQueued(OrderBookId("BTCEUR"), LimitOrder(OrderId("7"), now, CurrencyUnit("BTC"), 5.0, Money(106, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        OrderQueued(OrderBookId("BTCEUR"), LimitOrder(OrderId("8"), now, CurrencyUnit("BTC"), 5.0, Money(105, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        Nil
    }

    when {
      ob: OrderBook => ob.makeOrder(LimitOrder(OrderId("10"), now, CurrencyUnit("BTC"), 5.0, Money(10, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC")))
    }

    expected {
      OrdersExecuted(OrderBookId("BTCEUR"),
        LimitOrder(OrderId("10"), now, CurrencyUnit("BTC"), 5.0, Money(10, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC")),
        MarketOrder(OrderId("4"), now, CurrencyUnit("BTC"), 5.0, Money(101, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC")),
        Money(10, CurrencyUnit("EUR"))
      ) ::
        Nil
    }
  }
}

class OrderBook_T14 extends OrderBookTest {
  val now = new DateTime()

  test("take oldest market order for asks") {
    given(new OrderBookFactory()) {
      OrderBookCreated(OrderBookId("BTCEUR"), CurrencyUnit("EUR")) ::
        OrderQueued(OrderBookId("BTCEUR"), MarketOrder(OrderId("1"), now.minusMillis(123), CurrencyUnit("BTC"), 5.0, Money(100, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        OrderQueued(OrderBookId("BTCEUR"), MarketOrder(OrderId("2"), now.minusMillis(1243), CurrencyUnit("BTC"), 5.0, Money(110, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        OrderQueued(OrderBookId("BTCEUR"), MarketOrder(OrderId("3"), now.minusMillis(1343), CurrencyUnit("BTC"), 5.0, Money(111, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        OrderQueued(OrderBookId("BTCEUR"), MarketOrder(OrderId("4"), now.minusMillis(9243), CurrencyUnit("BTC"), 5.0, Money(101, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        OrderQueued(OrderBookId("BTCEUR"), MarketOrder(OrderId("5"), now.minusMillis(13), CurrencyUnit("BTC"), 5.0, Money(98, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        OrderQueued(OrderBookId("BTCEUR"), MarketOrder(OrderId("6"), now.minusMillis(12), CurrencyUnit("BTC"), 5.0, Money(100, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        OrderQueued(OrderBookId("BTCEUR"), MarketOrder(OrderId("7"), now.minusMillis(1), CurrencyUnit("BTC"), 5.0, Money(106, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        OrderQueued(OrderBookId("BTCEUR"), MarketOrder(OrderId("8"), now.minusMillis(0), CurrencyUnit("BTC"), 5.0, Money(105, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        Nil
    }

    when {
      ob: OrderBook => ob.makeOrder(LimitOrder(OrderId("10"), now, CurrencyUnit("BTC"), 5.0, Money(10, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC")))
    }

    expected {
      OrdersExecuted(OrderBookId("BTCEUR"),
        LimitOrder(OrderId("10"), now, CurrencyUnit("BTC"), 5.0, Money(10, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC")),
        MarketOrder(OrderId("4"), now.minusMillis(9243), CurrencyUnit("BTC"), 5.0, Money(101, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC")),
        Money(10, CurrencyUnit("EUR"))
      ) ::
        Nil
    }
  }
}

class OrderBook_T15 extends OrderBookTest {
  val now = new DateTime()

  test("take oldest market order  for bids") {
    given(new OrderBookFactory()) {
      OrderBookCreated(OrderBookId("BTCEUR"), CurrencyUnit("EUR")) ::
        OrderQueued(OrderBookId("BTCEUR"), MarketOrder(OrderId("1"), now.minusMillis(123), CurrencyUnit("BTC"), 5.0, Money(100, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        OrderQueued(OrderBookId("BTCEUR"), MarketOrder(OrderId("2"), now.minusMillis(435), CurrencyUnit("BTC"), 5.0, Money(110, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        OrderQueued(OrderBookId("BTCEUR"), MarketOrder(OrderId("3"), now.minusMillis(54), CurrencyUnit("BTC"), 5.0, Money(111, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        OrderQueued(OrderBookId("BTCEUR"), MarketOrder(OrderId("4"), now.minusMillis(34645), CurrencyUnit("BTC"), 5.0, Money(101, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        OrderQueued(OrderBookId("BTCEUR"), MarketOrder(OrderId("5"), now.minusMillis(234), CurrencyUnit("BTC"), 5.0, Money(98, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        OrderQueued(OrderBookId("BTCEUR"), MarketOrder(OrderId("6"), now.minusMillis(5467), CurrencyUnit("BTC"), 5.0, Money(100, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        OrderQueued(OrderBookId("BTCEUR"), MarketOrder(OrderId("7"), now.minusMillis(345), CurrencyUnit("BTC"), 5.0, Money(106, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        OrderQueued(OrderBookId("BTCEUR"), MarketOrder(OrderId("8"), now.minusMillis(345), CurrencyUnit("BTC"), 5.0, Money(105, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        Nil
    }

    when {
      ob: OrderBook => ob.makeOrder(LimitOrder(OrderId("10"), now, CurrencyUnit("BTC"), 5.0, Money(500, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC")))
    }

    expected {
      OrdersExecuted(OrderBookId("BTCEUR"),
        MarketOrder(OrderId("4"), now.minusMillis(34645), CurrencyUnit("BTC"), 5.0, Money(101, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC")),
        LimitOrder(OrderId("10"), now, CurrencyUnit("BTC"), 5.0, Money(500, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC")),
        Money(500, CurrencyUnit("EUR"))
      ) ::
        Nil
    }
  }
}

class OrderBook_T16 extends OrderBookTest {
  val now = new DateTime()

  test("order in order book has higher quantity than new order ") {
    given(new OrderBookFactory()) {
      OrderBookCreated(OrderBookId("BTCEUR"), CurrencyUnit("EUR")) ::
        OrderQueued(OrderBookId("BTCEUR"), LimitOrder(OrderId("1"), now, CurrencyUnit("BTC"), 5.0, Money(100, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        Nil
    }

    when {
      ob: OrderBook => ob.makeOrder(LimitOrder(OrderId("10"), now, CurrencyUnit("BTC"), 1.0, Money(100, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC")))
    }

    expected {
      OrdersExecuted(OrderBookId("BTCEUR"),
        LimitOrder(OrderId("1"), now, CurrencyUnit("BTC"), 1.0, Money(100, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC")),
        LimitOrder(OrderId("10"), now, CurrencyUnit("BTC"), 1.0, Money(100, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC")),
        Money(100, CurrencyUnit("EUR"))
      ) :: OrderAdjusted(OrderBookId("BTCEUR"), LimitOrder(OrderId("1"), now, CurrencyUnit("BTC"), 4.0, Money(100, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        Nil
    }
  }
}

class OrderBook_T17 extends OrderBookTest {
  val now = new DateTime()

  test("order in order book has lower quantity than new order") {
    given(new OrderBookFactory()) {
      OrderBookCreated(OrderBookId("BTCEUR"), CurrencyUnit("EUR")) ::
        OrderQueued(OrderBookId("BTCEUR"), LimitOrder(OrderId("1"), now, CurrencyUnit("BTC"), 1.0, Money(100, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        Nil
    }

    when {
      ob: OrderBook => ob.makeOrder(LimitOrder(OrderId("10"), now, CurrencyUnit("BTC"), 5.0, Money(100, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC")))
    }

    expected {
      OrdersExecuted(OrderBookId("BTCEUR"),
        LimitOrder(OrderId("1"), now, CurrencyUnit("BTC"), 1.0, Money(100, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC")),
        LimitOrder(OrderId("10"), now, CurrencyUnit("BTC"), 1.0, Money(100, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC")),
        Money(100, CurrencyUnit("EUR"))
      ) :: OrderQueued(OrderBookId("BTCEUR"), LimitOrder(OrderId("10"), now, CurrencyUnit("BTC"), 4.0, Money(100, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        Nil
    }
  }
}

class OrderBook_T18 extends OrderBookTest {
  val now = new DateTime()

  test("order in order book has lower quantity than new order") {
    given(new OrderBookFactory()) {
      OrderBookCreated(OrderBookId("BTCEUR"), CurrencyUnit("EUR")) ::
        OrderQueued(OrderBookId("BTCEUR"), LimitOrder(OrderId("1"), now, CurrencyUnit("BTC"), 1.0, Money(100, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        OrderQueued(OrderBookId("BTCEUR"), LimitOrder(OrderId("2"), now, CurrencyUnit("BTC"), 1.0, Money(100, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        OrderQueued(OrderBookId("BTCEUR"), LimitOrder(OrderId("3"), now, CurrencyUnit("BTC"), 1.0, Money(100, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        OrderQueued(OrderBookId("BTCEUR"), LimitOrder(OrderId("4"), now, CurrencyUnit("BTC"), 1.0, Money(100, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        OrderQueued(OrderBookId("BTCEUR"), LimitOrder(OrderId("5"), now, CurrencyUnit("BTC"), 2.0, Money(100, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        Nil
    }

    when {
      ob: OrderBook => ob.makeOrder(LimitOrder(OrderId("10"), now, CurrencyUnit("BTC"), 5.0, Money(100, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC")))
    }

    expected {
      OrdersExecuted(OrderBookId("BTCEUR"),
        LimitOrder(OrderId("1"), now, CurrencyUnit("BTC"), 1.0, Money(100, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC")),
        LimitOrder(OrderId("10"), now, CurrencyUnit("BTC"), 1.0, Money(100, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC")),
        Money(100, CurrencyUnit("EUR"))) ::
        OrdersExecuted(OrderBookId("BTCEUR"),
          LimitOrder(OrderId("2"), now, CurrencyUnit("BTC"), 1.0, Money(100, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC")),
          LimitOrder(OrderId("10"), now, CurrencyUnit("BTC"), 1.0, Money(100, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC")),
          Money(100, CurrencyUnit("EUR"))) ::
        OrdersExecuted(OrderBookId("BTCEUR"),
          LimitOrder(OrderId("3"), now, CurrencyUnit("BTC"), 1.0, Money(100, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC")),
          LimitOrder(OrderId("10"), now, CurrencyUnit("BTC"), 1.0, Money(100, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC")),
          Money(100, CurrencyUnit("EUR"))) ::
        OrdersExecuted(OrderBookId("BTCEUR"),
          LimitOrder(OrderId("4"), now, CurrencyUnit("BTC"), 1.0, Money(100, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC")),
          LimitOrder(OrderId("10"), now, CurrencyUnit("BTC"), 1.0, Money(100, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC")),
          Money(100, CurrencyUnit("EUR"))) ::
        OrdersExecuted(OrderBookId("BTCEUR"),
          LimitOrder(OrderId("5"), now, CurrencyUnit("BTC"), 1.0, Money(100, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC")),
          LimitOrder(OrderId("10"), now, CurrencyUnit("BTC"), 1.0, Money(100, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC")),
          Money(100, CurrencyUnit("EUR"))) ::
        OrderAdjusted(OrderBookId("BTCEUR"), LimitOrder(OrderId("5"), now, CurrencyUnit("BTC"), 1.0, Money(100, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        Nil
    }
  }
}

class OrderBook_T19 extends OrderBookTest {
  val now = new DateTime()

  test("order in order book has higher quantity than new order order gets crossed and quened") {
    given(new OrderBookFactory()) {
      OrderBookCreated(OrderBookId("BTCEUR"), CurrencyUnit("EUR")) ::
        OrderQueued(OrderBookId("BTCEUR"), LimitOrder(OrderId("1"), now, CurrencyUnit("BTC"), 1.0, Money(100, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        OrderQueued(OrderBookId("BTCEUR"), LimitOrder(OrderId("2"), now, CurrencyUnit("BTC"), 1.0, Money(100, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        OrderQueued(OrderBookId("BTCEUR"), LimitOrder(OrderId("3"), now, CurrencyUnit("BTC"), 1.0, Money(100, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        OrderQueued(OrderBookId("BTCEUR"), LimitOrder(OrderId("4"), now, CurrencyUnit("BTC"), 1.0, Money(100, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        OrderQueued(OrderBookId("BTCEUR"), LimitOrder(OrderId("5"), now, CurrencyUnit("BTC"), 1.0, Money(100, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        Nil
    }

    when {
      ob: OrderBook => ob.makeOrder(LimitOrder(OrderId("10"), now, CurrencyUnit("BTC"), 6.0, Money(100, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC")))
    }

    expected {
      OrdersExecuted(OrderBookId("BTCEUR"),
        LimitOrder(OrderId("1"), now, CurrencyUnit("BTC"), 1.0, Money(100, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC")),
        LimitOrder(OrderId("10"), now, CurrencyUnit("BTC"), 1.0, Money(100, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC")),
        Money(100, CurrencyUnit("EUR"))) ::
        OrdersExecuted(OrderBookId("BTCEUR"),
          LimitOrder(OrderId("2"), now, CurrencyUnit("BTC"), 1.0, Money(100, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC")),
          LimitOrder(OrderId("10"), now, CurrencyUnit("BTC"), 1.0, Money(100, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC")),
          Money(100, CurrencyUnit("EUR"))) ::
        OrdersExecuted(OrderBookId("BTCEUR"),
          LimitOrder(OrderId("3"), now, CurrencyUnit("BTC"), 1.0, Money(100, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC")),
          LimitOrder(OrderId("10"), now, CurrencyUnit("BTC"), 1.0, Money(100, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC")),
          Money(100, CurrencyUnit("EUR"))) ::
        OrdersExecuted(OrderBookId("BTCEUR"),
          LimitOrder(OrderId("4"), now, CurrencyUnit("BTC"), 1.0, Money(100, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC")),
          LimitOrder(OrderId("10"), now, CurrencyUnit("BTC"), 1.0, Money(100, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC")),
          Money(100, CurrencyUnit("EUR"))) ::
        OrdersExecuted(OrderBookId("BTCEUR"),
          LimitOrder(OrderId("5"), now, CurrencyUnit("BTC"), 1.0, Money(100, CurrencyUnit("EUR")), Buy, AccountId("1-EUR"), AccountId("1-BTC")),
          LimitOrder(OrderId("10"), now, CurrencyUnit("BTC"), 1.0, Money(100, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC")),
          Money(100, CurrencyUnit("EUR"))) ::
        OrderQueued(OrderBookId("BTCEUR"), LimitOrder(OrderId("10"), now, CurrencyUnit("BTC"), 1.0, Money(100, CurrencyUnit("EUR")), Sell, AccountId("1-EUR"), AccountId("1-BTC"))) ::
        Nil
    }
  }
}

class OrderBook_T0 extends FunSuite {
  val now = new DateTime()
  test("initiate place order") {
    val book = new OrderBookFactory().create(OrderBookId("EURBTC"), CurrencyUnit("EUR"))
      .markCommitted
      .placeOrder(TransactionId("1"), LimitOrder(OrderId("1"), now, CurrencyUnit("BTC"), 5.0, Money(100, CurrencyUnit("EUR")), Buy, AccountId("1"), AccountId("1")))

    assert(book.uncommittedEventsReverse.head === OrderPlaced(OrderBookId("EURBTC"), TransactionId("1"), LimitOrder(OrderId("1"), now, CurrencyUnit("BTC"), 5.0, Money(100, CurrencyUnit("EUR")), Buy, AccountId("1"), AccountId("1"))))
  }

  test("prepare an order - ok") {
    val book = new OrderBookFactory().create(OrderBookId("EURBTC"), CurrencyUnit("EUR"))
      .markCommitted
      .prepareOrderPlacement(OrderId("1"), TransactionId("1"), LimitOrder(OrderId("1"), now, CurrencyUnit("BTC"), 5.0, Money(100, CurrencyUnit("EUR")), Buy, AccountId("1"), AccountId("1")))

    assert(book.uncommittedEventsReverse.head === OrderPlacementPrepared(OrderBookId("EURBTC"), TransactionId("1"), OrderId("1"), LimitOrder(OrderId("1"), now, CurrencyUnit("BTC"), 5.0, Money(100, CurrencyUnit("EUR")), Buy, AccountId("1"), AccountId("1"))))
  }

  /*
  test("confirm an order - ok") {
    val book = new OrderBookFactory().create(OrderBookId("EURBTC"), CurrencyUnit("EUR"))
      .prepareOrderPlacement(OrderId("1"),TransactionId("1"),  LimitOrder(OrderId("1"),now, CurrencyUnit("BTC"), 5.0, Money(100, CurrencyUnit("EUR")), Buy, AccountId("1"), AccountId("1")))
      .markCommitted
      .confirmOrderPlacement(OrderId("1"), TransactionId("1") )

    assert(book.uncommittedEventsReverse.head === OrderPlacementConfirmed(OrderBookId("EURBTC"), TransactionId("1"), OrderId("1")))
  }
  */
}
