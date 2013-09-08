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

package org.kixi.myeventstore


import akka.actor.{Props, ActorLogging, Actor, ActorRef}
import eventstore.StreamEventAppeared


case class SubscribeMsg(subscriber: ActorRef, filter: Any => Boolean)

class AkkaEventHandler extends Actor with ActorLogging {
  var eventSubscribers: List[ActorRef] = Nil
  var filters: Map[ActorRef, Any => Boolean] = Map.empty[ActorRef, Any => Boolean]

  def receive = {
    case m: SubscribeMsg => {
      eventSubscribers = m.subscriber :: eventSubscribers
      filters += (m.subscriber -> m.filter)
    }
    case e =>
      for {
        sub <- eventSubscribers
        filter <- filters.get(sub) if filter(e)
      } {
        //    log.debug("Notifying actor: " + sub + " - Message "+e)
        sub ! e
      }
  }
}

object GYEventStoreHandler {
  def props(eventHandler: ActorRef) = Props(classOf[GYEventStoreHandler], eventHandler)
}

class GYEventStoreHandler(eventHandler: ActorRef) extends Actor with ActorLogging {
  def receive = {
    case msg: StreamEventAppeared =>
      try {
        eventHandler ! JavaSerializer.readObject(msg.event.event.data.data.toArray)
      } catch {
        case e: Throwable => log.warning(s"Unable to process message $msg", e)
      }
  }
}
