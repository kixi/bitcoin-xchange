/*
 * Copyright (c) 2013, Günter Kickinger.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * All advertising materials mentioning features or use of this software must
 * display the following acknowledgement: “This product includes software developed
 * by Günter Kickinger and his contributors.”
 * Neither the name of Günter Kickinger nor the names of its contributors may be
 * used to endorse or promote products derived from this software without specific
 * prior written permission.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS “AS IS”
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package cqrs

trait SpecTest[AR <: AggregateRoot[AR, E, I], ARF <: AggregateFactory[AR, E, I], E <: Event[I], I <: Identity] {
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
