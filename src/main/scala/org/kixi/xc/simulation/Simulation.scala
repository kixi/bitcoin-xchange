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

package org.kixi.xc.simulation

import akka.actor._
import scala.concurrent.duration._
import akka.actor.ActorIdentity
import scala.Some
import akka.actor.Identify
import org.kixi.xc.core.orderbook.domain.{OrderBookId, CreateOrderBook}
import org.kixi.xc.core.common.CurrencyUnit
import org.kixi.xc.core.account.domain.{MoneyDeposited, AccountOpened, AccountId}
import org.kixi.myeventstore.SubscribeMsg
import org.kixi.xc.service.ServiceEnvironment

/**
 * User: guenter
 * Date: 20.06.13
 * Time: 08:49
 */
object Simulator {

  def main(args: Array[String]) {
    ServiceEnvironment.buildEnvironment()

    Thread.sleep(100)


    val env = SimulationEnvironment.buildEnvironment

    val cons = System.console()

    Thread.sleep(100)

    for (user <- env.users) {
      user ! StartSimulation
    }

    System.in.read

    ServiceEnvironment.system.shutdown()
  }

}

case class SimulationEnvironment(system: ActorSystem, commandBus: ActorRef, eventHandler: ActorRef, users: List[ActorRef])

object SimulationEnvironment {
  def buildEnvironment = {
    val system = ActorSystem("bitcoin-xchange") //, ConfigFactory.load.getConfig("bitcoin-xchange-client"))
    //    val remotePathCommandBus = "akka.tcp://bitcoin-xchange@127.0.0.1:2552/user/command-bus"
    //    val remotePathEventHandler = "akka.tcp://bitcoin-xchange@127.0.0.1:2552/user/event-handler"
    val commandBus = ServiceEnvironment.commandBus
    val eventHandler = ServiceEnvironment.handler
    //    val commandBus = system.actorOf(Props(new LookupActor(remotePathCommandBus)), "commandbus-client")
    //    val eventHandler = system.actorOf(Props(new LookupActor(remotePathEventHandler)), "event-handler-client")

    Thread.sleep(100)

    commandBus ! CreateOrderBook(OrderBookId("BTCEUR"), CurrencyUnit("EUR"))
    Thread.sleep(100)
    val users =
      for (userId <- 0 to 9) yield {
        val user = system.actorOf(Props(new User(commandBus, userId)), "sim-user-" + userId)
        val accBtcId = AccountId(userId + "-BTC")
        val accEurId = AccountId(userId + "-EUR")

        eventHandler ! SubscribeMsg(user, (x) => x match {
          case AccountOpened(accountId, _, _, _) if accountId == accBtcId => true
          case AccountOpened(accountId, _, _, _) if accountId == accEurId => true
          case MoneyDeposited(accountId, _, _, _) if accountId == accBtcId => true
          case MoneyDeposited(accountId, _, _, _) if accountId == accEurId => true

          case _ => false
        })
        user
      }
    SimulationEnvironment(system, commandBus, eventHandler, users.toList)
  }
}

class LookupActor(path: String) extends Actor {

  context.setReceiveTimeout(3.seconds)
  sendIdentifyRequest()

  def sendIdentifyRequest(): Unit =
    context.actorSelection(path) ! Identify(path)

  def receive = {
    case ActorIdentity(`path`, Some(actor)) ⇒
      context.setReceiveTimeout(Duration.Undefined)
      context.become(active(actor))
    case ActorIdentity(`path`, None) ⇒ println(s"Remote actor not availible: $path")
    case ReceiveTimeout ⇒ sendIdentifyRequest()
    case _ ⇒ println("Not ready yet")
  }

  def active(actor: ActorRef): Actor.Receive = {
    case cmd => actor ! cmd
  }
}


