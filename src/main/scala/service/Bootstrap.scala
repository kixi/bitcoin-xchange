package service

import com.weiglewilczek.slf4s.Logger
import akka.actor.{Props, ActorSystem}
import util._
import domain.orderbook.OrderBookAppService
import domain.CommandBus
import projections.{LoggingProjection, AccountProjection}
import domain.sagas.{ProcessPaymentsSagaRouter, PlaceOrderSagaRouter}
import com.typesafe.config.ConfigFactory
import util.SubscribeMsg
import domain.account.AccountProcessor

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

    System.in.read

    ServiceEnvironment.system.shutdown()
  }
}

object ServiceEnvironment {
  val system = ActorSystem("bitcoin-xchange",  ConfigFactory.load.getConfig("bitcoin-xchange"))
  val handler = system.actorOf(Props(new SynchronousEventHandler()), "event-handler")
  val eventStore = new InMemoryEventStore(system, handler)
  val eventStoreActor =  system.actorOf(Props(classOf[FileEventStoreActor], handler).withDispatcher("my-pinned-dispatcher"), "event-store")
//  val accountService = system.actorOf(Props(new AccountAppService(eventStore)), "account-service" )
  val accountProcessor = system.actorOf(Props(new AccountProcessor(eventStoreActor)), "account-processor" )
  val orderBookService = system.actorOf(Props(new OrderBookAppService(eventStore)), "orderbook-service" )
  val commandBus = system.actorOf(Props(new CommandBus(eventStoreActor, accountProcessor, orderBookService)), "command-bus")
  val accountView = system.actorOf(Props(new AccountProjection()), "account-projection")


  def buildEnvironment = {

    handler ! SubscribeMsg(accountView, (x) => true)
    handler ! SubscribeMsg(system.actorOf(Props(new LoggingProjection()), "logging-projection"), (x) => true)
    handler ! SubscribeMsg(system.actorOf(Props(new PlaceOrderSagaRouter(commandBus)), "place-order-saga-router"), (x) => true)
    handler ! SubscribeMsg(system.actorOf(Props(new ProcessPaymentsSagaRouter(commandBus)), "process-payments-saga-router"), (x) => true)
  }
}


