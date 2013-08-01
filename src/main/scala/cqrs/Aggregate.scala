package cqrs

/**
 * Created with IntelliJ IDEA.
 * User: guenter
 * Date: 01.07.13
 * Time: 21:51
 * To change this template use File | Settings | File Templates.
 */

trait EventSourced[ES <: EventSourced[ES, E], E] {
  def applyEvent(e: E): ES
}


trait AggregateRoot[AR <: AggregateRoot[AR, E], E] extends EventSourced[AR, E] {
  def uncommittedEventsReverse : List[E]
  def version : Int

  def markCommitted: AR
  def uncommittedEvents = uncommittedEventsReverse.reverse
}

trait AggregateFactory[AR <: AggregateRoot[AR, E], E] extends EventSourced[AR, E] {
  def restoreFromHistory(events : List[E]): AR = {
    var aggregate: AR = applyEvent(events.head)
    for(event <- events.tail) {
      aggregate = aggregate.applyEvent(event)
    }
    aggregate.markCommitted
  }
}

