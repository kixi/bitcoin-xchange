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

package eventstore

import org.scalatest.FunSuite
import domain._
import com.weiglewilczek.slf4s.Logger
import java.io._
import domain.CurrencyUnit
import domain.AccountId
import domain.Money
import domain.AccountOpened
import cqrs.{Event, Identity}
/*
class EventStoreTest extends FunSuite {
  test("add one event and receive same event to file event store") {
    val store = new FileEventStore[Event[Identity]]("/data/")

    val events = AccountOpened(AccountId("1"), CurrencyUnit("EUR"), Money(0, CurrencyUnit("EUR"))) :: Nil

    store.appendEventsToStream("TEST-1", 0, events)

    val stored = store.loadEventStream("TEST-1")

    assert(events === stored)
  }

  test("add two events of same type and receive same events to file event store") {
    val store = new FileEventStore("/data/")

    val events = AccountOpened(AccountId("1"), CurrencyUnit("EUR"), Money(0, CurrencyUnit("EUR"))) :: AccountOpened(AccountId("2"), CurrencyUnit("EUR"), Money(0, CurrencyUnit("EUR"))) :: Nil

    store.appendEventsToStream("TEST-2", 0, events)

    val stored = store.loadEventStream("TEST-2")

    assert(events === stored)
  }

  test("add two events of different type and receive same events to file event store") {
    val store = new FileEventStore("/data/")

    val events = AccountOpened(AccountId("1"), CurrencyUnit("EUR"), Money(0, CurrencyUnit("EUR"))) ::
      MoneyDeposited(AccountId("1"), Money(100, CurrencyUnit("EUR")), new Money(100, CurrencyUnit("EUR"))) :: Nil

    store.appendEventsToStream("TEST-3", 0, events)

    val stored = store.loadEventStream("TEST-3")

    assert(events === stored)
  }

}

     */