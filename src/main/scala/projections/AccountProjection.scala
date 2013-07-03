package projections

import domain._
import domain.OrderPlaced
import domain.MoneyDeposited
import domain.AccountOpened
import akka.actor.Actor

/**
 * Created with IntelliJ IDEA.
 * User: guenter
 * Date: 20.06.13
 * Time: 13:56
 * To change this template use File | Settings | File Templates.
 */

case class ListAccounts(dummy: Int)

class AccountProjection extends Actor {

  val accounts = scala.collection.mutable.Map[AccountId, Money]()

  def receive = {
      case evt: MoneyDeposited => when(evt)
      case evt: AccountOpened => when(evt)
      case cmd: ListAccounts => {
        sender ! accounts.toList
       }
  }

  def when(e: MoneyDeposited) {
    accounts(e.id) = e.balance(CurrencyUnit("EUR"))
  }

  def when(e: AccountOpened) {
    accounts put (e.id, e.balance(CurrencyUnit("EUR")))
  }


}
