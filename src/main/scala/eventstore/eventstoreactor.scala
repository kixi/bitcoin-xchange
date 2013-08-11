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

package eventstore

import akka.actor._
import akka.dispatch.{PriorityGenerator, UnboundedPriorityMailbox}
import com.typesafe.config.Config

case class EventStream[E](events: List[E], streamVersion: StreamRevision)

case class AppendEventsToStream[E](streamId: String, expectedVersion: StreamRevision, events: List[E], boomerang: Any)

case class LoadEventStream(streamId: String, boomerang: Any)

case class EventsCommitted[E](committed: Commit[E])

case class EventsConflicted[E](conflict: Conflict[E], boomerang: Any)

case class EventsLoaded[E](stream: EventStream[E], boomerang: Any)

class MyPrioMailbox[E](settings: ActorSystem.Settings, config: Config)
  extends UnboundedPriorityMailbox(
    // Create a new PriorityGenerator, lower prio means more important
    PriorityGenerator {
      // 'highpriority messages should be treated first if possible
      case msg: AppendEventsToStream[E] => 0

      // 'lowpriority messages should be treated last if possible
      case msg: LoadEventStream => 2

      // PoisonPill when no other left
      case PoisonPill => 3

      // We default to 1, which is in between high and low
      case otherwise => 1
    })

class EventStoreActor[+E](eventHandler: ActorRef, store: EventStore[E]) extends Actor with ActorLogging {

  def receive = {
    case msg@AppendEventsToStream(streamId, version, events, boomerang) =>
      log.debug("trying to commit to event store: {}", msg)
      store.appendEventsToStream(streamId, version, events.asInstanceOf[List[E]]) match {
        case Left(conflict) =>
          log.debug("commit failure: {}", conflict)
          sender ! EventsConflicted(conflict.asInstanceOf[Conflict[E]], boomerang)
        case Right(committed) =>
          log.debug("events have been committed to stream {}", committed)
          sender ! EventsCommitted(committed.asInstanceOf[Commit[E]])
          committed.events.foreach(eventHandler ! _)
      }
    case LoadEventStream(streamId, boomerang) =>
      log.debug("Read events from store - stream id=" + streamId)
      val events = store.loadEventStream(streamId)
      log.debug("events readed from store - stream id={} - events: {}", streamId, events)
      sender ! EventsLoaded(EventStream(events, StreamRevision(events.size)), boomerang)
  }
}
