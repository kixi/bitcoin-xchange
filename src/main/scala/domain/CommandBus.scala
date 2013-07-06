package domain

import akka.actor.{ActorLogging, Actor, ActorRef}

/**
 * Created with IntelliJ IDEA.
 * User: guenter
 * Date: 06.07.13
 * Time: 13:28
 * To change this template use File | Settings | File Templates.
 */
class CommandBus(accountService : ActorRef, orderBookService: ActorRef) extends Actor with ActorLogging {
  def receive = {
    case cmd: CreateOrderBook => orderBookService forward cmd
    case cmd: PlaceOrder => orderBookService forward cmd
    case cmd: PrepareOrderPlacement => orderBookService forward cmd
    case cmd: ConfirmOrderPlacement => orderBookService forward cmd
    case cmd: ConfirmMoneyWithdrawal => accountService forward cmd
    case cmd: RequestMoneyWithdrawal => accountService forward cmd
    case cmd: OpenAccount => accountService forward cmd
    case cmd: DepositMoney => accountService forward cmd
    case cmd => System.out.println("Unknown command " + cmd)
  }
}
