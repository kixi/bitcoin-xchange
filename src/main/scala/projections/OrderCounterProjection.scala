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

package projections

import akka.actor.Actor
import domain._
import org.joda.time.format.DateTimeFormat
import org.joda.time.DateTime
import domain.MoneyWithdrawalConfirmed
import domain.OrderQueued
import domain.OrdersExecuted
import domain.MoneyDeposited

/**
 * Created with IntelliJ IDEA.
 * User: WZBKKG
 * Date: 08.08.13
 * Time: 13:36
 * To change this template use File | Settings | File Templates.
 */
class OrderCounterProjection extends Actor {
  var ordersQueued = 0
  var ordersExecuted = 0
  var deposits = 0
  var withdrawals = 0
  var placements = 0

  var total = 0

  val displayAfter = 10000

  val fmt = DateTimeFormat.forPattern("HH:mm:ss:SSSS")

  def receive : Actor.Receive = {
    case _ : OrderQueued =>
      log
      ordersQueued += 1
    case _ : OrdersExecuted =>
      log
      ordersExecuted += 1
    case _ : MoneyDeposited =>
      log
      deposits += 1
    case _ : MoneyWithdrawalConfirmed =>
      log
      withdrawals += 1
    case _ : OrderPlaced =>
      log
      placements += 1
    case _ =>
  }

  def log {
    total += 1
    if (total % displayAfter == 0)
      System.out.println(fmt.print(new DateTime()) +" queued="+ordersQueued+", executed="+ordersExecuted + ", deposits="+deposits+", withdrawals="+withdrawals+", placements="+placements)
  }
}
