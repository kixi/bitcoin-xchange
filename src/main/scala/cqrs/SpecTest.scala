package cqrs

/**
 * Created with IntelliJ IDEA.
 * User: guenter
 * Date: 19.07.13
 * Time: 23:28
 * To change this template use File | Settings | File Templates.
 */
trait SpecTest[AR <: AggregateRoot[AR, E], ARF <: AggregateFactory[AR, E], E] {
  var aggregate: AR = _

  var exceptionCatched: Option[Throwable] = None

  def given(factory: ARF)(f: => List[E]) {
    aggregate = factory.restoreFromHistory(f)
  }

  def when(executeCommand: AR => AR) {
    try {
       aggregate = executeCommand(aggregate)
    } catch {
      case e: Throwable => exceptionCatched = Some(e)
    }
  }

  def expected(eventList: List[E]) = {
    if (!eventList.isEmpty) {
      var expectedEvents = eventList
      for (generatedEvent <- aggregate.uncommittedEvents) {
         if (!expectedEvents.isEmpty && generatedEvent != expectedEvents.head) {
          assert(false, "Expected event does not equal generated event! expected = " + expectedEvents.head + " generated = " + generatedEvent)
        }
        if (expectedEvents.isEmpty) {
          assert(false, "No expected event for generated event = "+expectedEvents)
        }
        if (!expectedEvents.isEmpty) {
          expectedEvents = expectedEvents.tail
        }
      }
    }
    if (!aggregate.uncommittedEvents.isEmpty) {
      var generatedEvents = aggregate.uncommittedEvents
      for (expectedEvent <- eventList) {
        if (!generatedEvents.isEmpty && expectedEvent != generatedEvents.head) {
          assert(false, "Expected event does not equal generated event! \nexpected = " + expectedEvent + "\ngenerated = " + generatedEvents.head)
        }
        if (generatedEvents.isEmpty) {
          assert(false, "No generated event for expected event = "+expectedEvent)
        }
        if (!generatedEvents.isEmpty) {
          generatedEvents = generatedEvents.tail
        }
      }
    }

  }

  def expected(exception: Throwable) {
    exceptionCatched match {
      case None => assert(false, "No exception occurred. Expected = " + exception)
      case Some(e) => assert(exception.getClass.getName == e.getClass.getName, "Unexpected exception! expected = " + exception + " thrown "+ e)
    }
  }
}
