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

case class SubscribeMsg(subscriber: ActorRef, filter: Any => Boolean)

class SynchronousEventHandler extends Actor {
  var eventSubscribers : List[ActorRef] = Nil
  var filters : Map[ActorRef, Any=>Boolean] = Map.empty[ActorRef, Any => Boolean]

   def receive = {
    case m:SubscribeMsg => {
      eventSubscribers = m.subscriber :: eventSubscribers
      filters += (m.subscriber -> m.filter)
    }
    case e =>
      for {
        sub <- eventSubscribers
        filter <- filters.get(sub) if (filter(e))
       } sub ! e
  }
}
