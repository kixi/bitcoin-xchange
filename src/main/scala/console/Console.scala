package console

import akka.pattern.{ ask, pipe }
import com.weiglewilczek.slf4s.Logger
import domain._
import domain.DepositMoney
import util.{SubscribeMsg, InMemoryEventStore, SynchronousEventHandler}
import projections.{ListAccounts, LoggingProjection, AccountProjection}
import akka.actor.{Props, ActorRef, ActorSystem}
import akka.util.Timeout
import akka.util.duration._
import akka.dispatch.Await
import domain.account.AccountAppService
import cqrs.EventStore
import domain.orderbook.OrderBookAppService

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
    while (true) {
      Thread.sleep(300)

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
          case e:RuntimeException => e.printStackTrace()
        }
      }
    }
  }
}

case class ConsoleEnvironment(account: ActorRef, orderbook: ActorRef, eventStore: EventStore, accountProjection: ActorRef) {
  val commandList = List(
    new DepositMoneyCmd(),
    new ListAccountsCmd(),
    new OpenAccountCmd(),
    new CreateOrderBookCmd(),
    new PlaceOrderCmd(),
    new ExitCmd())
  val commands: Map[String, ConsoleCommand] = {
    for (cmd <- commandList)
    yield (cmd.cmdString, cmd)
  }.toMap
}

object ConsoleEnvironment {
  def buildEnvironment = {
    val system = ActorSystem("Bitcoin-XChange")
    val handler = system.actorOf(Props(new SynchronousEventHandler()))

    val accountView = system.actorOf(Props(new AccountProjection()))
    handler ! SubscribeMsg(accountView)
    handler ! SubscribeMsg(system.actorOf(Props(new LoggingProjection())))

    val eventStore = new InMemoryEventStore(handler)

    val accountService = system.actorOf(Props(new AccountAppService(eventStore)) )
    val orderBookService = system.actorOf(Props(new OrderBookAppService(eventStore)) )

    ConsoleEnvironment(accountService, orderBookService, eventStore, accountView)
  }
}


trait ConsoleCommand {
  def cmdString: String

  def execute(env: ConsoleEnvironment, args: Array[String])
}

class OpenAccountCmd extends ConsoleCommand {
  override def cmdString = "openAccount"

  override def execute(env: ConsoleEnvironment, args: Array[String]) {
    env.account ! (OpenAccount(AccountId(args(0))))
  }
}

class DepositMoneyCmd extends ConsoleCommand {
  override def cmdString = "deposit"

  override def execute(env: ConsoleEnvironment, args: Array[String]) {
    env.account ! (DepositMoney(AccountId(args(0)), Money(BigDecimal(args(1)), CurrencyUnit(args(2)))))
  }
}

class CreateOrderBookCmd extends ConsoleCommand {
  override def cmdString = "createOrderBook"

  override def execute(env: ConsoleEnvironment, args: Array[String]) {
    env.orderbook ! CreateOrderBook(OrderBookId(args(0)))
  }
}

class PlaceOrderCmd extends ConsoleCommand {
  override def cmdString = "placeOrder"

  override def execute(env: ConsoleEnvironment, args: Array[String]) {
    env.orderbook ! PlaceOrder(OrderBookId(args(0)), LimitOrder(CurrencyUnit(args(1)), BigDecimal(args(2)), Money(BigDecimal(args(3)), CurrencyUnit(args(4))), AccountId(args(5))))
  }
}

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
class ExitCmd extends ConsoleCommand {
  override def cmdString = "exit"

  override def execute(env: ConsoleEnvironment, args: Array[String]) {
    System.exit(0)
  }
}

