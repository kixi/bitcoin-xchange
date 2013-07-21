package service

import com.weiglewilczek.slf4s.Logger
import akka.actor.{Props, ActorSystem}
import util.{SubscribeMsg, InMemoryEventStore, SynchronousEventHandler}
import domain.account.AccountAppService
import domain.orderbook.OrderBookAppService
import domain.CommandBus
import projections.{LoggingProjection, AccountProjection}
import domain.sagas.SagaRouter
import com.typesafe.config.ConfigFactory

/**
 * Created with IntelliJ IDEA.
 * User: guenter
 * Date: 20.07.13
 * Time: 10:12
 * To change this template use File | Settings | File Templates.
 */

object Service {
  val log = Logger("Console")

  def main(args: Array[String]) {
    log.info("Starting bitcoin-exchange service ...")

    ServiceEnvironment.buildEnvironment
    log.info("Starting bitcoin-exchange up and running")
  }
}

object ServiceEnvironment {
  def buildEnvironment = {
    val system = ActorSystem("bitcoin-xchange",  ConfigFactory.load.getConfig("bitcoin-xchange"))
    val handler = system.actorOf(Props(new SynchronousEventHandler()), "event-handler")
    val eventStore = new InMemoryEventStore(system, handler)
    val accountService = system.actorOf(Props(new AccountAppService(eventStore)), "account-service" )
    val orderBookService = system.actorOf(Props(new OrderBookAppService(eventStore)), "orderbook-service" )

    val commandBus = system.actorOf(Props(new CommandBus(accountService, orderBookService)), "command-bus")

    val accountView = system.actorOf(Props(new AccountProjection()), "account-projection")
    handler ! SubscribeMsg(accountView)
    handler ! SubscribeMsg(system.actorOf(Props(new LoggingProjection()), "logging-projection"))
    handler ! SubscribeMsg(system.actorOf(Props(new SagaRouter(commandBus)), "saga-router"))
  }
}


