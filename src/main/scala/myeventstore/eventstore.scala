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

package myeventstore


import scala.collection._
import com.weiglewilczek.slf4s.Logger
import scala.Some

/**
 * The revision of an event store. The revision of an event store is
 * equal to the number of commits in the event store.
 */
final case class StoreRevision(value: Long) extends Ordered[StoreRevision] {
  require(value >= 0, "store revision cannot be negative")

  def previous = StoreRevision(value - 1)
  def next = StoreRevision(value + 1)

  def +(that: Long): StoreRevision = StoreRevision(value + that)
  def -(that: Long): StoreRevision = StoreRevision(value - that)
  def -(that: StoreRevision): Long = this.value - that.value

  override def compare(that: StoreRevision) = value compare that.value
}
object StoreRevision {
  val Initial = StoreRevision(0)
  val Maximum = StoreRevision(Long.MaxValue)
}

/**
 * The revision of an event stream. The revision of an event stream is
 * equal to the number of commits in the event stream.
 */
final case class StreamRevision(value: Long) extends Ordered[StreamRevision] {
  require(value >= -1, "stream revision cannot be negative")

  def previous = StreamRevision(value - 1)
  def next = StreamRevision(value + 1)

  def +(that: Long): StreamRevision = StreamRevision(value + that)
  def -(that: Long): StreamRevision = StreamRevision(value - that)
  def -(that: StreamRevision): Long = this.value - that.value

  override def compare(that: StreamRevision) = value compare that.value
}

object StreamRevision {
  val Initial = StreamRevision(0)
  val Maximum = StreamRevision(Int.MaxValue)
  val NoConflict = StreamRevision(-1)
}
/**
 * A successful commit to `streamId`.
 */
case class Commit[+E](streamId: String, streamRevision: StreamRevision, events: Seq[E]) {
  def eventsWithRevision: Seq[(E, StreamRevision)] = events.map(event => (event, streamRevision))
}

/**
 * The conflict that occurred while trying to commit to `streamId`.
 */
case class Conflict[+E](streamId: String, actual: StreamRevision, expected: StreamRevision, storedEvents: List[E], newEvents: List[E])

trait EventStore[E] {
  def loadEventStream(streamId:String, since: StreamRevision = StreamRevision.Initial, to: StreamRevision = StreamRevision.Maximum):List[E]
  def appendEventsToStream(streamId: String, expectedVersion: StreamRevision, events: List[E]):CommitResult[E]
}

class FakeEventStore[E] extends EventStore[E]  {
  def loadEventStream(streamId: String, since: StreamRevision, to: StreamRevision): List[E] = List.empty[E]

  def appendEventsToStream(streamId: String, expectedVersion: StreamRevision, events: List[E]): CommitResult[E] = Right(Commit(streamId, expectedVersion+1, events))
}

class InMemoryEventStore[E] extends EventStore[E] {
  val store: mutable.Map[String, List[E]] = mutable.Map[String, List[E]]()

  val log = Logger("InMemoryEventStore")

  def appendEventsToStream(streamId: String, expectedVersion: StreamRevision, events: List[E]):CommitResult[E] = {
    val currentVersion = getVersion(streamId)
    if (expectedVersion != StreamRevision.NoConflict && currentVersion != expectedVersion) {
      val conflicting = loadEventStream(streamId, since = expectedVersion)
      Left(Conflict(streamId, currentVersion, expectedVersion, conflicting, events))
    } else {
      store.get(streamId) match {
        case Some(eventList) =>
          store(streamId) = eventList ::: events
          Right(Commit(streamId, currentVersion + events.size, events))
        case None =>
          store(streamId) = events
          Right(Commit(streamId, currentVersion + events.size, events))
      }
    }
  }

  def getVersion(streamId: String) = {
    StreamRevision(loadEventStream(streamId).size)
  }

  def loadEventStream(streamId: String, since: StreamRevision = StreamRevision.Initial, to: StreamRevision = StreamRevision.Maximum): List[E] = {
    val allEvents = store.getOrElse(streamId, Nil)

    val reduceEvents = allEvents.drop(since.value.toInt).take(to.value.toInt - since.value.toInt)

    reduceEvents
  }
}


/*
class FileEventStore(directory: String) extends EventStore {
  val log = Logger("FileEventStore")

  def using[C <: Closeable](closeable: C)(f: C => Unit) {
    try {
      f(closeable)
    } catch {
      case t: Throwable => log.error("Error", t)
    } finally {
      closeable.close()
    }
  }

  def appendEventsToStream(streamId: String, expectedVersion: Long, events: List[Event[Identity]]) {
    val currentVersion = getVersion(streamId)
    if (currentVersion != expectedVersion) {
      throw new ConcurrentStreamModificationException(streamId, expectedVersion, currentVersion, events)
    }
    using(objectOutputStream(streamId)) {
      (oos: ObjectOutputStream) => events.foreach(oos.writeObject(_))
    }
  }

  def getVersion(streamId: String) = {
    loadEventStream(streamId).size
  }

  def loadEventStream(streamId: String): List[Event[Identity]] = {
    var list = List.empty[Event[Identity]]
    if (!exists(fileName(streamId))) {
      list
    } else {
      using(new ObjectInputStream(new FileInputStream(fileName(streamId)))) {
        (ois: ObjectInputStream) => {
          try {
            while (true) {
              val obj = ois.readObject()
              list = obj.asInstanceOf[Event[Identity]] :: list
            }
          }
          catch {
            case e: EOFException => // do nothing
          }
        }
      }
      list.reverse
    }
  }

  def fileName(streamId: String) = {
    directory + streamId + ".ser"
  }

  def exists(file: String) = {
    new File(file).exists()
  }

  def objectOutputStream(streamId: String) = {
    if (exists(fileName(streamId))) {
      new AppendingObjectOutputStream(new FileOutputStream(fileName(streamId), true))
    } else {
      new ObjectOutputStream(new FileOutputStream(fileName(streamId)))
    }
  }
}


class AppendingObjectOutputStream(out: OutputStream) extends ObjectOutputStream(out) {
  override def writeStreamHeader() {
    // do not write a header, but reset:
    // this line added after another question
    // showed a problem with the original
    reset();
  }
}
       */
