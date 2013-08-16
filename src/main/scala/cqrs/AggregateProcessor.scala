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

import akka.actor.{ActorRef, ActorLogging, Actor}
import domain._
import myeventstore._
import myeventstore.EventStream
import myeventstore.LoadEventStream
import myeventstore.EventsCommitted
import myeventstore.AppendEventsToStream

/**
 * Created with IntelliJ IDEA.
 * User: guenter
 * Date: 03.08.13
 * Time: 07:42
 * To change this template use File | Settings | File Templates.
 */
abstract class AggregateProcessor[ARF <: AggregateFactory[AR, E, ARID], AR <: AggregateRoot[AR, E, ARID], ARID <: Identity, CMD <: Command[ARID], E <: Event[ARID]](eventStore: ActorRef) extends Actor with ActorLogging {

  def factory: ARF

  def receive = {
    case cmd: Command[Identity] =>
      log.debug("received {}", cmd)
      eventStore ! LoadEventStream(cmd.id.id, cmd)
    case EventsLoaded(stream, boomerang) =>
      log.debug("received stream {} {}", stream, boomerang)
      process(boomerang.asInstanceOf[CMD], stream.asInstanceOf[EventStream[Event[Identity]]])
    case EventsCommitted(e) =>
      log.debug("Events stored {}", e)
    case EventsConflicted(conflicted, boomerang) =>
      if (canResolveConflict(conflicted.asInstanceOf[Conflict[Event[Identity]]])) {
        log.debug("Resolved version conflict. conflict=" + conflicted)
        eventStore ! AppendEventsToStream(conflicted.streamId, conflicted.actual, conflicted.newEvents, boomerang)
      } else {
        log.debug("Unresolved version conflict - trying command " + boomerang + " again! conflict=" + conflicted)
        self ! boomerang
      }
    case msg => log.error("Unknown message " + msg)
  }

  private def canResolveConflict(conflicted: Conflict[Event[Identity]]) = {
    val conflicts =
      for {
        conf <- conflicted.storedEvents
        att <- conflicted.newEvents
        if (ConflictHandler.conflictsWith(conf, att))
      } yield conf
    conflicts.isEmpty
  }

  def process(command: CMD, stream: EventStream[Event[Identity]])

  def insert(stream: EventStream[Event[Identity]], cmd: CMD, f: Unit => AR) {
    if (stream.streamVersion == StreamRevision.Initial) {
      val aggregate = f()
      log.debug("Aggregate created - new uncommitted state {}", aggregate)
      write(cmd, aggregate)
    } else
      throw new RuntimeException("aggregate with this id already exists " + cmd.id)
  }

  def update(stream: EventStream[Event[Identity]], cmd: CMD, f: AR => AR) {

    if (stream.streamVersion != StreamRevision.Initial) {
      val restoredAggregate = factory.restoreFromHistory(stream.events.asInstanceOf[List[E]])
      log.debug("Aggregate reconstructed from eventstream {}", restoredAggregate)
      val newAggregate = f(restoredAggregate)
      log.debug("Aggregate processed - new uncommitted state {}", newAggregate)
      write(cmd, newAggregate)
    } else
      throw new RuntimeException("aggregate id = " + cmd.id + " does not exist ")
  }

  def write(cmd: CMD, aggregate: AR) {
    val revision: StreamRevision =
      if (neverCanConflict(aggregate.uncommittedEvents))
        StreamRevision.NoConflict
      else
        StreamRevision(aggregate.version)

    eventStore ! AppendEventsToStream(cmd.id.id, revision, aggregate.uncommittedEvents.asInstanceOf[List[Event[Identity]]], cmd)
  }

  def neverCanConflict(events: List[Event[Identity]]) = {
    (for (evt <- events if ConflictHandler.canConflict(evt)) yield evt).isEmpty
  }

}
