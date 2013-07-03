package cqrs

import domain.Event

/**
 * Created with IntelliJ IDEA.
 * User: guenter
 * Date: 19.06.13
 * Time: 23:35
 * To change this template use File | Settings | File Templates.
 */
trait EventStore {
  def loadEventStream(id:String):EventStream
  def appendEventsToStream(id: String, version: Int, events: List[Event])
}

case class EventStream(val events: List[Event], val streamVersion: Int)

