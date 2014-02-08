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

package org.kixi.xc.service

import com.weiglewilczek.slf4s.Logger
import akka.actor.{Props, ActorSystem}
import org.kixi.xc.common.appservice.CommandDispatcherActor
import org.kixi.myeventstore._
import org.kixi.xc.projections.{OrderCounterProjection, AccountProjection}
import org.kixi.xc.core.sagas.{ProcessPaymentsSagaRouter, PlaceOrderSagaRouter}

object Service {
  val log = Logger("Console")

  def main(args: Array[String]) {
    log.info("Starting bitcoin-exchange service ...")

    ServiceEnvironment.buildEnvironment()
    log.info("Starting bitcoin-exchange up and running")

    System.in.read

    ServiceEnvironment.system.shutdown()
  }
}

object ServiceEnvironment {
  val system = ActorSystem("bitcoin-xchange")

  val handler = system.actorOf(Props(classOf[AkkaEventHandler]), "event-bus")
  val commandDispatcher = system.actorOf(CommandDispatcherActor.props(handler), "command-bus")

  val accountView = system.actorOf(Props(classOf[AccountProjection]), "account-projection")
  val counter = system.actorOf(Props(classOf[OrderCounterProjection]), "order-counter-projection")

  def buildEnvironment() {
    //   handler ! SubscribeMsg(accountView, (x) => true)
    handler ! SubscribeMsg(system.actorOf(Props(classOf[PlaceOrderSagaRouter], commandDispatcher), "place-order-saga-router"), (x) => true)
    handler ! SubscribeMsg(system.actorOf(Props(classOf[ProcessPaymentsSagaRouter], commandDispatcher), "process-payments-saga-router"), (x) => true)
  }
}


