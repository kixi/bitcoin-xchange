package simulation

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
import util.SubscribeMsg

/**
 * Created with IntelliJ IDEA.
 * User: guenter
 * Date: 20.06.13
 * Time: 08:49
 * To change this template use File | Settings | File Templates.
 */
object Simulator {
  val log = Logger("Simujlator")

  def main(args: Array[String]) {
    log.info("Starting bitcoin-exchange simulator cliewnt ...")

    val env = SimulationEnvironment.buildEnvironment

    val cons = System.console()

    Thread.sleep(5000)

    for (user <- env.users) {
      user ! StartSimulation
    }
    System.in.read
  }

}

case class SimulationEnvironment(system: ActorSystem, commandBus: ActorRef, eventHandler: ActorRef, users : List[ActorRef])

object SimulationEnvironment {
  def buildEnvironment = {
    val system = ActorSystem("bitcoin-xchange", ConfigFactory.load.getConfig("bitcoin-xchange-client"))
    val remotePathCommandBus = "akka.tcp://bitcoin-xchange@127.0.0.1:2552/user/command-bus"
    val remotePathEventHandler = "akka.tcp://bitcoin-xchange@127.0.0.1:2552/user/event-handler"
    val commandBus = system.actorOf(Props(new LookupActor(remotePathCommandBus)), "commandbus-client")
    val eventHandler = system.actorOf(Props(new LookupActor(remotePathEventHandler)), "event-handler-client")

    Thread.sleep(5000)

    commandBus ! CreateOrderBook(OrderBookId("BTCEUR"), CurrencyUnit("EUR"))
    Thread.sleep(100)
    val users =
    for (userId <- 0 to 1000) yield {
      val user = system.actorOf(Props(new User(commandBus, userId)),"sim-user-"+userId)
      val accBtcId = AccountId(userId+"-BTC")
      val accEurId = AccountId(userId+"-EUR")
      /*
       eventHandler ! SubscribeMsg(user, (x) =>  x match {
        case AccountOpened(accountId, _, _) if (accountId == accBtcId ) => true
        case AccountOpened(accountId, _, _) if (accountId == accEurId) => true
        case MoneyDeposited(accountId, _) if (accountId == accBtcId ) => true
        case MoneyDeposited(accountId, _) if (accountId == accEurId) => true

        case _ => false
      } )    */
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


