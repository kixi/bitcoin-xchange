package domain.orderbook

import akka.actor.Actor
import domain.account.AccountFactory
import domain._
import domain.OpenAccount
import domain.account.Account
import domain.DepositMoney
import domain.AccountId
import cqrs.{EventStore, ApplicationService}

/**
  * Created with IntelliJ IDEA.
  * User: guenter
  * Date: 19.06.13
  * Time: 23:35
  * To change this template use File | Settings | File Templates.
  */
class OrderBookAppService(val eventStore: EventStore) extends Actor with ApplicationService[OrderBook, OrderBookId, OrderBookFactory, OrderBookEvent]{

   val factory = new OrderBookFactory()

   override def receive = {
     case c: CreateOrderBook => insert(c.id, (Unit) => factory.create(c.id, c.currency))
     case c: PlaceOrder => update(c.id, _.placeOrder(c.transactionId, c.order))
     case c: PrepareOrderPlacement => update(c.id, _.prepareOrderPlacement(c.orderId, c.transactionId, c.order))
     case c: ConfirmOrderPlacement => update(c.id, _.confirmOrderPlacement(c.orderId, c.transactionId))
   }

  }
