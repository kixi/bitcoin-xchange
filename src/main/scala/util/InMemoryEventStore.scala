package util

import domain.{Event}
import scala.collection._
import scala.concurrent.Await
import akka.pattern.ask
import scala.concurrent.duration._
import akka.actor.{Actor, Props, ActorRef, ActorSystem}
import cqrs.{EventStream, EventStore}
import akka.util.Timeout

/**
 * Created with IntelliJ IDEA.
 * User: guenter
 * Date: 20.06.13
 * Time: 14:09
 * To change this template use File | Settings | File Templates.
 */
class InMemoryEventStore(as: ActorSystem, val eventHandler: ActorRef) extends EventStore {

  lazy val logger = as.actorOf(Props(new EventStoreActor(eventHandler)))
  implicit val timeout = Timeout(20 seconds)

  def loadEventStream(id: String): EventStream = {
    val future = logger ? LoadEventStream(id)
    Await.result(future, timeout.duration).asInstanceOf[EventStream]
  }

  def appendEventsToStream(id: String, version: Int, events: List[Event]) {
    logger ! AppendEventsToStream(id, version, events)
  }
}

case class AppendEventsToStream(streamId: String, version: Int, events: List[Event])
case class LoadEventStream(streamId: String)

class EventStoreActor(eventHandler: ActorRef) extends Actor {
  val store : mutable.Map[String, List[Event]] = mutable.Map[String, List[Event]]()

  def receive = {
    case AppendEventsToStream(streamId, version, events) => {
      store.get(streamId) match {
        case Some(eventList) => store(streamId) = eventList ::: events
        case None => store(streamId) = events
      }
      events.foreach(eventHandler ! _)
    }

    case LoadEventStream(streamId) => {
      val list = store.getOrElse(streamId, Nil)
      sender ! EventStream(list, list.size)
    }

  }
}
