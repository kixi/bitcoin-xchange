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

package org.kixi.xc.console

import com.weiglewilczek.slf4s.Logger
import akka.actor._
import scala.concurrent.duration._
import org.joda.time.DateTime
import org.kixi.xc.core.common._
import org.kixi.xc.core.orderbook.domain.CreateOrderBook
import org.kixi.xc.core.common.Money
import org.kixi.xc.core.account.domain.OpenAccount
import akka.actor.ActorIdentity
import scala.Some
import org.kixi.xc.core.account.domain.DepositMoney
import akka.actor.Identify
import org.kixi.xc.core.common.CurrencyUnit
import org.kixi.xc.core.common.LimitOrder
import org.kixi.xc.core.account.domain.AccountId
import org.kixi.xc.core.orderbook.domain.OrderBookId
import org.kixi.xc.core.orderbook.domain.PlaceOrder
import org.kixi.xc.core.common.TransactionId
import org.kixi.xc.service.ServiceEnvironment

object Console {
  val log = Logger("Console")

  def main(args: Array[String]) {
    log.info("Starting bitcoin-exchange console ...")

    val env = ConsoleEnvironment.buildEnvironment

    val cons = System.console()

    Thread.sleep(500)

    /*    for(cmd <- env.commands.get("createOrderBook")) cmd.execute(env, "BTCEUR EUR".split(" "))
        for(cmd <- env.commands.get("openAccount")) cmd.execute(env, "1-EUR EUR".split(" "))
        for(cmd <- env.commands.get("openAccount")) cmd.execute(env, "1-BTC BTC".split(" "))
        for(cmd <- env.commands.get("openAccount")) cmd.execute(env, "2-EUR EUR".split(" "))
        for(cmd <- env.commands.get("openAccount")) cmd.execute(env, "2-BTC BTC".split(" "))
        for(cmd <- env.commands.get("deposit")) cmd.execute(env, "1-EUR 10000 EUR".split(" "))
        for(cmd <- env.commands.get("deposit")) cmd.execute(env, "1-BTC 50 BTC".split(" "))
        for(cmd <- env.commands.get("deposit")) cmd.execute(env, "2-EUR 8000 EUR".split(" "))
        for(cmd <- env.commands.get("deposit")) cmd.execute(env, "2-BTC 100 BTC".split(" "))
     */
    while (true) {
      Thread.sleep(30)

      cons.printf("btcx $ ")
      val line = cons.readLine()
      if (!line.trim.isEmpty) {
        val command = line.split(" ")
        try {
          env.commands.get(command.head) match {
            case Some(cmd) => cmd.execute(env, command.tail)
            case _ => cons.printf("Invalid command: %s\n", command.head)
          }
        } catch {
          case e: RuntimeException => e.printStackTrace()
        }
      }
    }
  }
}

case class ConsoleEnvironment(commandBus: ActorRef) {
  val commandList = List(
    new DepositMoneyCmd(),
    //   new ListAccountsCmd(),
    new OpenAccountCmd(),
    new CreateOrderBookCmd(),
    new BuyCmd(),
    new SellCmd(),
    new ExitCmd())
  val commands: Map[String, ConsoleCommand] = {
    for (cmd <- commandList)
    yield (cmd.cmdString, cmd)
  }.toMap
}

object ConsoleEnvironment {
  def buildEnvironment = {
    val system = ActorSystem("bitcoin-xchange") //, ConfigFactory.load.getConfig("bitcoin-xchange-client"))
    //   val remotePath =
    //     "akka.tcp://bitcoin-xchange@127.0.0.1:2552/user/command-bus"
    //   val commandBus = system.actorOf(Props(new LookupActor(remotePath)), "commandbus-client")
    val commandBus = ServiceEnvironment.commandBus
    val eventHandler = ServiceEnvironment.handler

    ConsoleEnvironment(commandBus)
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

trait ConsoleCommand {
  def cmdString: String

  def execute(env: ConsoleEnvironment, args: Array[String])
}

class OpenAccountCmd extends ConsoleCommand {
  override def cmdString = "openAccount"

  override def execute(env: ConsoleEnvironment, args: Array[String]) {
    env.commandBus ! OpenAccount(AccountId(args(0)), CurrencyUnit(args(1)))
  }
}

class DepositMoneyCmd extends ConsoleCommand {
  override def cmdString = "deposit"

  override def execute(env: ConsoleEnvironment, args: Array[String]) {
    env.commandBus ! DepositMoney(AccountId(args(0)), Money(BigDecimal(args(1)), CurrencyUnit(args(2))))
  }
}

class CreateOrderBookCmd extends ConsoleCommand {
  override def cmdString = "createOrderBook"

  override def execute(env: ConsoleEnvironment, args: Array[String]) {
    env.commandBus ! CreateOrderBook(OrderBookId(args(0)), CurrencyUnit(args(1)))
  }
}

class BuyCmd extends ConsoleCommand {
  override def cmdString = "buy"

  override def execute(env: ConsoleEnvironment, args: Array[String]) {
    env.commandBus ! PlaceOrder(OrderBookId("BTCEUR"), TransactionId(),
      LimitOrder(OrderId(),
        new DateTime(),
        CurrencyUnit("BTC"),
        BigDecimal(args(0)),
        Money(BigDecimal(args(1)), CurrencyUnit(args(2))),
        Buy,
        AccountId(args(3) + "-EUR"),
        AccountId(args(3) + "-BTC")))

  }
}

class SellCmd extends ConsoleCommand {
  override def cmdString = "sell"

  override def execute(env: ConsoleEnvironment, args: Array[String]) {
    env.commandBus ! PlaceOrder(OrderBookId("BTCEUR"), TransactionId(),
      LimitOrder(OrderId(),
        new DateTime(),
        CurrencyUnit("BTC"),
        BigDecimal(args(0)),
        Money(BigDecimal(args(1)), CurrencyUnit(args(2))),
        Sell,
        AccountId(args(3) + "-EUR"),
        AccountId(args(3) + "-BTC")))

  }
}

/*
class ListAccountsCmd extends ConsoleCommand {
  override def cmdString = "listAccounts"

  override def execute(env: ConsoleEnvironment, args: Array[String]) {
    implicit val timeout = Timeout(1 seconds)
    val futureRes = (env.accountProjection ? ListAccounts(5))
    val list = Await.result(futureRes, 1 seconds).asInstanceOf[List[(AccountId, Money)]]
    for {(id, balance) <- list}
        System.console.printf("%10s %10s %3s\n", id, balance.amount.toString, balance.currency.iso)
   }
}
*/
class ExitCmd extends ConsoleCommand {
  override def cmdString = "exit"

  override def execute(env: ConsoleEnvironment, args: Array[String]) {
    System.exit(0)
  }
}

