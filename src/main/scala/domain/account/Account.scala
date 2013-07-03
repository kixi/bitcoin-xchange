package domain.account

import domain._
import domain.account.Account
import domain.account.Account
import domain.account.Account
import domain.account.Account
import domain.account.Account
import domain.UnhandledEventException
import domain.account.Account
import domain.Balances
import domain.MoneyWithdrawn
import domain.AccountId
import domain.InsufficientFundsException
import domain.Money
import domain.MoneyDeposited
import domain.AccountOpened
import cqrs.{AggregateFactory, AggregateRoot}

/**
 * Created with IntelliJ IDEA.
 * User: guenter
 * Date: 19.06.13
 * Time: 22:24
 * To change this template use File | Settings | File Templates.
 */

class AccountFactory extends AggregateFactory[Account, AccountEvent] {

  def create(id: AccountId) = applyEvent(AccountOpened(id, new Balances()))

  override def applyEvent(e: AccountEvent) = {
    e match {
      case event: AccountOpened => new Account(event :: Nil, 0, event.id, event.balance)
      case event => throw new UnhandledEventException("Aggregate Account does not handle event " + event)
    }
  }
}

case class Account(uncommittedEvents:List[AccountEvent], version: Int, id: AccountId, balance: Balances) extends AggregateRoot[Account, AccountEvent] {


  def markCommitted = copy(uncommittedEvents = Nil)

  def withdrawMoney(amount: Money): Account = {
    ensureSufficientFunds(amount)
    applyEvent(MoneyWithdrawn(id, balance - amount))
  }

  def depositMoney(amount: Money): Account = {
    applyEvent(MoneyDeposited(id, balance + amount))
  }

  def ensureSufficientFunds(amount: Money) {
    if (balance(amount.currency) < amount)
      throw new InsufficientFundsException("Not enough funds to place order")
  }

  def applyEvent(e: AccountEvent) = {
    e match {
      case event: AccountOpened => when(event)
      case event: MoneyDeposited => when(event)
      case event: MoneyWithdrawn => when(event)
      case event => throw new UnhandledEventException("Aggregate Account does not handle event " + event)
    }
  }

  def when(e: AccountOpened): Account = {
    Account(e :: uncommittedEvents,0, e.id,  e.balance)
  }

  def when(e: MoneyDeposited): Account =  {
    copy(uncommittedEvents = e :: uncommittedEvents, balance = e.balance)
  }

  def when(e: MoneyWithdrawn): Account =  {
    copy(uncommittedEvents = e :: uncommittedEvents, balance = e.balance)
  }
}

