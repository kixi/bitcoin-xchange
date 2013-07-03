package util

import domain.{Event}
import scala.collection._
import akka.actor.ActorRef
import cqrs.{EventStream, EventStore}

/**
 * Created with IntelliJ IDEA.
 * User: guenter
 * Date: 20.06.13
 * Time: 14:09
 * To change this template use File | Settings | File Templates.
 */
class InMemoryEventStore(val eventHandler: ActorRef) extends EventStore {

  val store : mutable.Map[String, List[Event]] = mutable.Map[String, List[Event]]()

  def loadEventStream(id: String): EventStream = {
    val list = store.getOrElse(id, Nil)
    EventStream(list, list.size)
  }

  def appendEventsToStream(id: String, version: Int, events: List[Event]) {
    store.get(id) match {
      case Some(eventList) =>  store(id) = eventList ::: events
      case None => store(id) = events
    }
    events.foreach(eventHandler ! _)
  }
}
