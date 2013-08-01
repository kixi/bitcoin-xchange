package cqrs

import domain.{Event, AggregateNotFoundException, Identity}
import com.weiglewilczek.slf4s.Logger
import commons.StopWatch

/**
 * Created with IntelliJ IDEA.
 * User: guenter
 * Date: 02.07.13
 * Time: 21:55
 * To change this template use File | Settings | File Templates.
 */
trait ApplicationService[AR <: AggregateRoot[AR, E], ARID <: Identity, ARF<: AggregateFactory[AR, E], E] {
  def eventStore:EventStore
  def factory: ARF

  val logger = Logger("ApplicationService")
  def read(id: ARID):Option[AR] = {
    val eventStreamId = id.id
    val eventStream = eventStore.loadEventStream(eventStreamId)
    if (eventStream.streamVersion == 0) {
      None
    } else {
      Some(factory.restoreFromHistory(eventStream.events.asInstanceOf[List[E]]))
    }
  }

  def update(id: ARID, f : AR => AR) {
    val watch = new StopWatch
    read(id) match {
      case None => throw new AggregateNotFoundException("aggregate with id " + id + " not found")
      case Some(aggregate) => {
        val newAggregate = f(aggregate)
        eventStore.appendEventsToStream(id.id, aggregate.version, newAggregate.uncommittedEvents.asInstanceOf[List[Event]])
      }
    }
    logger.debug("update in " + watch.stop)
  }

  def insert(id: ARID, f : Unit => AR) {
    val watch = new StopWatch
    read(id) match {
      case None => {
        val aggregate = f()
        eventStore.appendEventsToStream(id.id, aggregate.version, aggregate.uncommittedEvents.asInstanceOf[List[Event]])
      }
      case Some(aggregate) => throw new RuntimeException("account with this id already exists" + id)
    }
    logger.debug("insert in " + watch.stop)
  }

}
