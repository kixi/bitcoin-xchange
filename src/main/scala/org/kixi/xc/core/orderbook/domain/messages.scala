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

package org.kixi.xc.core.orderbook.domain

import org.joda.time.DateTime
import org.kixi.cqrslib.aggregate.{Event, Command}
import org.kixi.xc.core.common._
import org.kixi.xc.core.common.CurrencyUnit
import org.kixi.xc.core.account.domain.TransactionId


trait OrderBookCommand extends Command[OrderBookId]

trait OrderBookEvent extends Event[OrderBookId]


case class CreateOrderBook(id: OrderBookId, currency: CurrencyUnit, timestamp: DateTime = new DateTime()) extends OrderBookCommand

case class OrderBookCreated(id: OrderBookId, currency: CurrencyUnit, timestamp: DateTime = new DateTime()) extends OrderBookEvent

case class PlaceOrder(id: OrderBookId, transactionId: TransactionId, order: LimitOrder, timestamp: DateTime = new DateTime()) extends OrderBookCommand

case class OrderPlaced(id: OrderBookId, transactionId: TransactionId, order: LimitOrder, timestamp: DateTime = new DateTime()) extends OrderBookEvent

case class PrepareOrderPlacement(id: OrderBookId, transactionId: TransactionId, orderId: OrderId, order: LimitOrder, timestamp: DateTime = new DateTime()) extends OrderBookCommand

case class OrderPlacementPrepared(id: OrderBookId, transactionId: TransactionId, orderId: OrderId, order: LimitOrder, timestamp: DateTime = new DateTime()) extends OrderBookEvent

case class ConfirmOrderPlacement(id: OrderBookId, transactionId: TransactionId, orderId: OrderId, order: LimitOrder, timestamp: DateTime = new DateTime()) extends OrderBookCommand

case class OrderPlacementConfirmed(id: OrderBookId, transactionId: TransactionId, orderId: OrderId, timestamp: DateTime = new DateTime()) extends OrderBookEvent


case class OrderQueued(id: OrderBookId, order: Order, timestamp: DateTime = new DateTime()) extends OrderBookEvent

case class OrderAdjusted(id: OrderBookId, order: Order, timestamp: DateTime = new DateTime()) extends OrderBookEvent

case class OrdersExecuted(id: OrderBookId, buy: Order, sell: Order, price: Money, timestamp: DateTime = new DateTime()) extends OrderBookEvent

case class PricesChanged(id: OrderBookId, bidPrice: BigDecimal, askPrice: BigDecimal, timestamp: DateTime = new DateTime()) extends OrderBookEvent

case class OrderExpired(id: OrderBookId, order: Order, timestamp: DateTime = new DateTime()) extends OrderBookEvent

