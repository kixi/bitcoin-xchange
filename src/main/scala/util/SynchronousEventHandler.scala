package util

import domain.Event
import akka.actor.{Actor, ActorRef}

/**
 * Created with IntelliJ IDEA.
 * User: guenter
 * Date: 20.06.13
 * Time: 12:47
 * To change this template use File | Settings | File Templates.
 */

case class SubscribeMsg(subscriber: ActorRef)

class SynchronousEventHandler extends Actor {
   var eventSubscribers : List[ActorRef] = Nil

   def receive = {
    case m:SubscribeMsg => eventSubscribers = m.subscriber :: eventSubscribers
    case e => eventSubscribers.foreach(_ ! e)
  }
}
