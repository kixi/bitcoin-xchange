package util


/**
 * Created with IntelliJ IDEA.
 * User: guenter
 * Date: 20.06.13
 * Time: 14:09
 * To change this template use File | Settings | File Templates.
 */
/*
class RedisEventStore(as: ActorSystem, val eventHandler: ActorRef) extends EventStore {

  lazy val logger = as.actorOf(Props(new EventStoreActor(eventHandler)).withDispatcher("my-pinned-dispatcher"))
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
      new ObjectOutputStream(new ByteArrayOutputStream(bytes)).
      clients.withClient {client =>
        client.lpush(id, events.)
      }

    case LoadEventStream(streamId) =>
      import Parse.Implicits.parseByteArray
      val entries =
        clients.withClient {client =>
          client.lrange[Event](streamId, 0, -1)
        }
      val ren = entries.map(_.map(e => e.get)).getOrElse(List.empty[Event]).reverse
      sender ! ren
  }
}
*/