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
/*
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
           */