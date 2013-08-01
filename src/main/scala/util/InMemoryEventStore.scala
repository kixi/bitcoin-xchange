package util

import domain.Event
import scala.collection._
import scala.concurrent.Await

import akka.pattern.ask
import scala.concurrent.duration._
import akka.actor.{Actor, Props, ActorRef, ActorSystem}
import cqrs.{EventStream, EventStore}
import akka.util.Timeout
import com.weiglewilczek.slf4s.Logger
import java.io.{ObjectInputStream, FileInputStream, ObjectOutputStream, FileOutputStream}

/**
 * Created with IntelliJ IDEA.
 * User: guenter
 * Date: 20.06.13
 * Time: 14:09
 * To change this template use File | Settings | File Templates.
 */
class InMemoryEventStore(as: ActorSystem, val eventHandler: ActorRef) extends EventStore {

  lazy val eventLogger = as.actorOf(Props(new InMemoryEventStoreActore(eventHandler)))
  val log = Logger("InMemoryeventStore")
  implicit val timeout = Timeout(20 seconds)

  def loadEventStream(id: String): EventStream = {
   val future = eventLogger ? LoadEventStream(id, null)
    val res = Await.result(future, timeout.duration).asInstanceOf[EventStream]
   res
  }

  def appendEventsToStream(id: String, version: Int, events: List[Event]) {
    eventLogger ! AppendEventsToStream(id, version, events)
  }
}

case class AppendEventsToStream(streamId: String, version: Int, events: List[Event])
case class LoadEventStream(streamId: String, boomerang: Any)
case class EventsCommitted(streamId: String)

class InMemoryEventStoreActore(eventHandler: ActorRef) extends Actor {
  val store : mutable.Map[String, List[Event]] = mutable.Map[String, List[Event]]()

  def receive = {
    case AppendEventsToStream(streamId, version, events) => {
      store.get(streamId) match {
        case Some(eventList) => store(streamId) = eventList ::: events
        case None => store(streamId) = events
      }
      sender ! EventsCommitted(streamId)
      events.foreach(eventHandler ! _)
    }

    case LoadEventStream(streamId, boomerang) => {
      val list = store.getOrElse(streamId, Nil)
      sender ! EventStream(list, list.size, boomerang)
    }

  }
}

class FileEventStoreActor(eventHandler: ActorRef) extends Actor {
  val directory = "c:/data/eventstore/"
  def receive = {
    case AppendEventsToStream(streamId, version, events) => {
      val fos = new FileOutputStream(directory+streamId+".ser", true)
      val oos = new ObjectOutputStream(fos)

      events.foreach(oos.writeObject(_))
      fos.close()

      sender ! EventsCommitted(streamId)
      events.foreach(eventHandler ! _)
    }

    case LoadEventStream(streamId, boomerang) => {
      var list : List[Event]  = List.empty[Event]
      try {
      val fis = new FileInputStream(directory+streamId+".ser")
      val ois = new ObjectInputStream(fis)
      var cont = true
      while(cont) {
        val obj = ois.readObject()
        if (obj == null)
          cont = false
        else
          list = obj.asInstanceOf[Event] :: list
      }
      } catch {
        case _ =>
      }

      sender ! EventStream(list.reverse, list.size, boomerang)
    }

  }
}
