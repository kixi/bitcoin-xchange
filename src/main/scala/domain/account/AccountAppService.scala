package domain.account

import akka.actor.Actor
import domain.account.{AccountFactory, Account}
import domain._
import domain.account.Account
import domain.account.Account
import domain.OpenAccount
import domain.account.Account
import domain.DepositMoney
import domain.AccountId
import cqrs.{EventStore, ApplicationService}

/**
 * Created with IntelliJ IDEA.
 * User: guenter
 * Date: 19.06.13
 * Time: 23:35
 * To change this template use File | Settings | File Templates.
 */
class AccountAppService(val eventStore: EventStore) extends Actor with ApplicationService[Account, AccountId, AccountFactory, AccountEvent]{

  val factory = new AccountFactory()

  override def receive = {
    case c: OpenAccount => when(c)
    case c: DepositMoney => when(c)
    case c: RequestMoneyWithdrawal => when(c)
    case c: ConfirmMoneyWithdrawal => when(c)
  }

  def when(cmd: DepositMoney) {
    update(cmd.id, (a:Account) => a.depositMoney(cmd.amount))
  }

  def when(cmd: RequestMoneyWithdrawal) {
    update(cmd.id, (a:Account)=> a.requestMoneyWithdrawal(cmd.transactionId, cmd.amount))
  }

  def when(cmd: ConfirmMoneyWithdrawal) {
    update(cmd.id, (a:Account)=> a.confirmMoneyWithdrawal(cmd.transactionId))
  }

  def when(cmd: OpenAccount) {
    insert(cmd.id, (Unit) =>  factory.create(cmd.id))
  }

 }
