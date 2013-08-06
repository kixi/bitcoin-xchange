package console

import akka.pattern.{ask, pipe}
import com.weiglewilczek.slf4s.Logger
import akka.actor._
import com.typesafe.config.ConfigFactory
import domain._
import scala.Some
import scala.concurrent.duration._
import org.joda.time.DateTime
import domain.CreateOrderBook
import domain.Money
import akka.actor.ActorIdentity
import domain.OpenAccount
import scala.Some
import domain.DepositMoney
import akka.actor.Identify
import domain.CurrencyUnit
import domain.LimitOrder
import domain.AccountId
import domain.OrderBookId
import domain.PlaceOrder
import domain.TransactionId

/**
 * Created with IntelliJ IDEA.
 * User: guenter
 * Date: 20.06.13
 * Time: 08:49
 * To change this template use File | Settings | File Templates.
 */
object Console {
  val log = Logger("Console")

  def main(args: Array[String]) {
    log.info("Starting bitcoin-exchange console ...")

    val env = ConsoleEnvironment.buildEnvironment

    val cons = System.console()

    Thread.sleep(5000)

    for(cmd <- env.commands.get("createOrderBook")) cmd.execute(env, "BTCEUR EUR".split(" "))
 /*   for(cmd <- env.commands.get("openAccount")) cmd.execute(env, "1-EUR EUR".split(" "))
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
    val system = ActorSystem("bitcoin-xchange", ConfigFactory.load.getConfig("bitcoin-xchange-client"))
    val remotePath =
      "akka.tcp://bitcoin-xchange@127.0.0.1:2552/user/command-bus"
    val commandBus = system.actorOf(Props(new LookupActor(remotePath)), "commandbus-client")

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
    env.commandBus ! (OpenAccount(AccountId(args(0)), CurrencyUnit(args(1))))
  }
}

class DepositMoneyCmd extends ConsoleCommand {
  override def cmdString = "deposit"

  override def execute(env: ConsoleEnvironment, args: Array[String]) {
    env.commandBus ! (DepositMoney(AccountId(args(0)), Money(BigDecimal(args(1)), CurrencyUnit(args(2)))))
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
        AccountId(args(3)+"-EUR"),
        AccountId(args(3)+"-BTC")))

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
        AccountId(args(3)+"-EUR"),
        AccountId(args(3)+"-BTC")))

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

