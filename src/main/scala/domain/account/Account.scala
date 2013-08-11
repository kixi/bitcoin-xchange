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

package domain.account

import domain._
import domain.UnhandledEventException
import domain.AccountId
import domain.InsufficientFundsException
import domain.Money
import domain.MoneyDeposited
import domain.AccountOpened
import cqrs.{AggregateFactory, AggregateRoot}
import com.weiglewilczek.slf4s.Logger

/**
 * Created with IntelliJ IDEA.
 * User: guenter
 * Date: 19.06.13
 * Time: 22:24
 * To change this template use File | Settings | File Templates.
 */

class AccountFactory extends AggregateFactory[Account, AccountEvent, AccountId] {

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
                    balance: Money) extends AggregateRoot[Account, AccountEvent, AccountId] {

   val log = Logger("Account")

  def markCommitted = copy(uncommittedEventsReverse = Nil)

  def loadedVersion(version: Int) = {
    copy(version = version)
  }

  def requestMoneyWithdrawal(withdrawalId: TransactionId, amount: Money): Account = {
    ensureAmountIsPositive(amount)
    ensureSufficientFunds(amount, withdrawalId)
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
    ensureAmountIsPositive(amount)
    ensureCurrenciesMatch(amount)
    applyEvent(MoneyDeposited(id, amount, balance + amount))
  }

  def ensureSufficientFunds(amount: Money, transactionId: TransactionId) {
    if (balance < amount) {
       throw new InsufficientFundsException("" + this +". Not enough funds to place order "+transactionId+". Amount requested: "+ amount)
    }
  }

  def ensureCurrenciesMatch(amount: Money) {
    if (currency != amount.currency) {
      throw new InvalidCurrencyException("Currencies do not match ")
    }
  }

  def ensureAmountIsPositive(amount: Money) {
    if (amount.amount <= 0) {
      throw new InvalidAmountException("Amount must be positive but was " + amount)
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
    Account(e :: uncommittedEventsReverse,1, e.id, e.currency, Map.empty[TransactionId, Money], e.balance)
  }

  def when(e: MoneyDeposited): Account =  {
    copy(uncommittedEventsReverse = e :: uncommittedEventsReverse, balance = e.balance)
  }

  def when(e: MoneyWithdrawalRequested): Account =  {
    if (e.balance.amount < 0) {
      log.error("" + this +" balance is smaller than 0!!!")
    }
    copy(uncommittedEventsReverse = e :: uncommittedEventsReverse, requestedWithdrawals=requestedWithdrawals + (e.transactionId -> e.amount), balance = e.balance)
  }

  def when(e: MoneyWithdrawalConfirmed): Account =  {
    copy(uncommittedEventsReverse = e :: uncommittedEventsReverse, requestedWithdrawals=requestedWithdrawals - (e.transactionId))
  }

}

