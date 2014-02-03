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

package org.kixi.xc.core.common

import org.kixi.cqrslib.aggregate.{Identity, Event}
import org.kixi.xc.core.orderbook.domain.{OrderBookEvent, OrderAdjusted, OrdersExecuted}
import org.kixi.xc.core.account.domain.{MoneyWithdrawn, MoneyDeposited}

/**
 * User: guenter
 * Date: 17.08.13
 * Time: 18:05
 */
object ConflictHandler {
  def canConflict(event: Event[Identity]) = {
    event match {
      case _: OrdersExecuted => true
      case _: OrderAdjusted => true
      case _: OrderBookEvent => false
      case _ => true
    }
  }

  def conflictsWith(committed: Event[Identity], attempted: Event[Identity]): Boolean = (committed, attempted) match {
    case (_, a: OrdersExecuted) => true
    case (_, a: OrderAdjusted) => true
    case (c: OrderBookEvent, a: OrderBookEvent) => false
    case (_: MoneyDeposited, _: MoneyWithdrawn) => true
    case (_: MoneyWithdrawn, _: MoneyDeposited) => true

    case (c, a) => (c.getClass == a.getClass) && (c.id == a.id)
  }
}
