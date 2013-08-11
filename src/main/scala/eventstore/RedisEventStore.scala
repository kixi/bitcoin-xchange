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
 /*
import akka.actor.{Actor, Props, ActorRef, ActorSystem}
import cqrs.{EventStream, EventStore}
import scala.concurrent.duration._
import scala.concurrent.Await
import domain.Event
import akka.eventstore.Timeout
import java.io.{ByteArrayOutputStream, ObjectOutputStream}
import com.typesafe.config.ConfigException.Parse
import com.redis.RedisClientPool

  */
/**
 * Created with IntelliJ IDEA.
 * User: guenter
 * Date: 20.06.13
 * Time: 14:09
 * To change this template use File | Settings | File Templates.
 */
 /*
class RedisEventStore(as: ActorSystem, val eventHandler: ActorRef) extends EventStore {

  lazy val logger = as.actorOf(Props(new InMemoryEventStoreActore(eventHandler)).withDispatcher("my-pinned-dispatcher"))
  implicit val timeout = Timeout(20 seconds)

  def loadEventStream(id: String): EventStream = {
    val future = logger ? LoadEventStream(id)
    Await.result(future, Timeout(20 seconds).duration).asInstanceOf[EventStream]
  }

  def appendEventsToStream(id: String, version: Int, events: List[Event]) {
    logger ! AppendEventsToStream(id, version, events)
  }
}

case class AppendEventsToStream(streamId: String, version: Int, events: List[Event])
case class LoadEventStream(streamId: String)

class Logger(clients: RedisClientPool) extends Actor {

  def receive = {
    case AppendEventsToStream(id, version, events) =>
  //    new ObjectOutputStream(new ByteArrayOutputStream(bytes)).
      clients.withClient {client =>
        client.lpush(id, events)
      }

    case LoadEventStream(streamId) =>

      val entries =
        clients.withClient {client =>
          client.lrange[Event](streamId, 0, -1)
        }
      val ren = entries.map(_.map(e => e.get)).getOrElse(List.empty[Event]).reverse
      sender ! ren
  }
}                                                                                                     */