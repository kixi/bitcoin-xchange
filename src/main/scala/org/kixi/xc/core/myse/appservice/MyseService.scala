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

package org.kixi.xc.core.myse.appservice

import akka.actor.{Props, Actor, ActorLogging}
import org.kixi.xc.core.myse.domain.{MyseEvent, PlaceOrder, MyseId, Myse}
import org.kixi.myeventstore.{StreamRevision, AppendEventsToStream}
import org.kixi.cqrslib.aggregate.Event

/**
 * Created by centos on 2/2/14.
 */

object MyseService {
  def props(bridge: Props) = Props(classOf[MyseService], bridge)
}
class MyseService(bridge: Props) extends Actor with ActorLogging{
  var myse : Myse = new Myse(id = MyseId("MYSE"))
  val bridgeActor = context.actorOf(bridge, "bridge-" + myse.id.id)

  def receive = {
    case cmd: PlaceOrder => {
      myse = myse.process(cmd)
      val events = myse.uncommittedEventsReverse.reverse
      publish(events)
    }
  }
  private def publish(events : List[MyseEvent]) {
    bridgeActor ! AppendEventsToStream(myse.id.id, StreamRevision.NoConflict, events, None)
    myse = myse.markCommitted
  }

}
