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

package org.kixi.xc.performance

import org.kixi.xc.service.ServiceEnvironment
import akka.actor.{ActorSystem, Props, ActorRef, Actor}
import org.kixi.xc.core.orderbook.domain._
import org.kixi.xc.core.account.domain.{AccountId, TransactionId}
import org.kixi.xc.core.common.{Money, CurrencyUnit}
import org.joda.time.DateTime
import org.kixi.xc.core.common.Money
import org.kixi.xc.core.orderbook.domain.OrderBookId
import org.kixi.xc.core.common.CurrencyUnit
import org.kixi.xc.core.orderbook.domain.OrderId
import org.kixi.xc.core.orderbook.domain.LimitOrder
import org.kixi.xc.core.account.domain.TransactionId
import org.kixi.xc.core.account.domain.AccountId
import org.kixi.xc.core.orderbook.domain.ProcessOrder
import org.kixi.myeventstore.{AkkaEventHandler, SubscribeMsg}
import org.kixi.xc.common.appservice.CommandDispatcherActor

/**
 * Created by centos on 2/9/14.
 */
object Performance {
  val system = ActorSystem("bitcoin-xchange")
  val perf = ServiceEnvironment.system.actorOf(Props(classOf[RequestSuperVisor], 10000))
  val commandDispatcher = system.actorOf(CommandDispatcherActor.props(perf), "command-bus")

  def main(args: Array[String]) = {

    perf ! Start
    System.in.read
    system.shutdown()
  }
}

class RequestSuperVisor(requests: Int) extends Actor {

  var start = System.nanoTime()
  var remainingReq = requests
  System.out.println(s"start: $start")

  def receive: Receive = {
    case Start =>
      start = System.nanoTime()
      send()
    case e: OrderProcessed =>
      val now = System.nanoTime()
      val duration = (now - start) / 1000
      start = now
      remainingReq -= 1
      System.out.println(s"$duration")
      if (remainingReq > 0)
        send()
  }

  def send() = {
    val order = LimitOrder(
      OrderId(),
      new DateTime(),
      CurrencyUnit("BTC"),
      1,
      Money(1000, CurrencyUnit("EUR")) ,
      Buy,
      AccountId("1"),
      AccountId("1")
    )
    Performance.commandDispatcher ! ProcessOrder(
      OrderBookId("BTCEUR"),
      TransactionId(),
      order)
  }
}

object Start

