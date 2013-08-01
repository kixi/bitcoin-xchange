package domain.account

import domain._
import domain.UnhandledEventException
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

  def create(id: AccountId, currency: CurrencyUnit) = applyEvent(AccountOpened(id, currency, new Money(0, currency)))

  override def applyEvent(e: AccountEvent) = {
    e match {
      case event: AccountOpened => new Account(event :: Nil, 0, event.id, event.currency, Map.empty[TransactionId, Money], event.balance)
      case event => throw new UnhandledEventException("Aggregate Account does not handle event " + event)
    }
  }
}

case class Account(
                    uncommittedEventsReverse:List[AccountEvent],
                    version: Int,
                    id: AccountId,
                    currency: CurrencyUnit,
                    requestedWithdrawals:Map[TransactionId, Money],
                    balance: Money) extends AggregateRoot[Account, AccountEvent] {


  def markCommitted = copy(uncommittedEventsReverse = Nil)

  def requestMoneyWithdrawal(withdrawalId: TransactionId, amount: Money): Account = {
    ensureSufficientFunds(amount)
    ensureCurrenciesMatch(amount)
    applyEvent(MoneyWithdrawalRequested(id, withdrawalId, amount, balance - amount))
  }

  def confirmMoneyWithdrawal(withdrawalId: TransactionId): Account = {
    if (!requestedWithdrawals.contains(withdrawalId)) {
      throw new InvalidWithdrawalException("Withdrawal with id " + id +" was not found. Cannot confirm it")
    }
    applyEvent(MoneyWithdrawalConfirmed(id, withdrawalId))
  }

  def depositMoney(amount: Money): Account = {
    ensureCurrenciesMatch(amount)
    applyEvent(MoneyDeposited(id, balance + amount))
  }

  def ensureSufficientFunds(amount: Money) {
    if (balance < amount)
      throw new InsufficientFundsException("Not enough funds to place order. Current balance :"+ balance +". Amount requested:"+amount)
  }

  def ensureCurrenciesMatch(amount: Money) {
    if (currency != amount.currency) {
      throw new InvalidCurrencyException("Currencies do not match ")
    }
  }
  def applyEvent(e: AccountEvent) = {
    e match {
      case event: AccountOpened => when(event)
      case event: MoneyDeposited => when(event)
      case event: MoneyWithdrawalRequested => when(event)
      case event: MoneyWithdrawalConfirmed => when(event)
      case event => throw new UnhandledEventException("Aggregate Account does not handle event " + event)
    }
  }

  def when(e: AccountOpened): Account = {
    Account(e :: uncommittedEventsReverse,0, e.id, e.currency, Map.empty[TransactionId, Money], e.balance)
  }

  def when(e: MoneyDeposited): Account =  {
    copy(uncommittedEventsReverse = e :: uncommittedEventsReverse, balance = e.balance)
  }

  def when(e: MoneyWithdrawalRequested): Account =  {
    copy(uncommittedEventsReverse = e :: uncommittedEventsReverse, requestedWithdrawals=requestedWithdrawals + (e.transactionId -> e.amount), balance = e.balance)
  }

  def when(e: MoneyWithdrawalConfirmed): Account =  {
    copy(uncommittedEventsReverse = e :: uncommittedEventsReverse, requestedWithdrawals=requestedWithdrawals - (e.transactionId))
  }

}

