package domain

import akka.actor._

/**
 * Created with IntelliJ IDEA.
 * User: guenter
 * Date: 06.07.13
 * Time: 13:28
 * To change this template use File | Settings | File Templates.
 */
class CommandBus(eventStore: ActorRef, accountProcessor : ActorRef, orderBookService: ActorRef) extends Actor with ActorLogging {
  def receive = {
    case cmd: OrderBookCommand => orderBookService ! cmd
    case cmd: AccountCommand => accountProcessor ! cmd

    case cmd => System.out.println("Unknown command " + cmd)
  }
}
