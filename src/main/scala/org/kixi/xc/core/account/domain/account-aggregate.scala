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

package org.kixi.xc.core.account.domain

import com.weiglewilczek.slf4s.Logger
import org.kixi.cqrslib.aggregate.{Identity, AggregateFactory, AggregateRoot}
import org.kixi.xc.core.common._
import java.util.UUID
import org.kixi.xc.core.common.UnhandledEventException
import org.kixi.xc.core.common.InsufficientFundsException
import org.kixi.xc.core.common.InvalidAmountException
import org.kixi.xc.core.common.InvalidCurrencyException


case class AccountId(id: String = UUID.randomUUID().toString) extends Identity

class AccountFactory extends AggregateFactory[Account, AccountEvent, AccountId] {

  def create(id: AccountId, currency: CurrencyUnit) = applyEvent(AccountOpened(id, currency))

  override def applyEvent(e: AccountEvent) = {
    e match {
      case event: AccountOpened => new Account(event :: Nil, 0, event.id, event.currency, Money(0, event.currency))
      case event => throw new UnhandledEventException("Aggregate Account does not handle event " + event)
    }
  }
}

case class Account(
                    uncommittedEventsReverse: List[AccountEvent],
                    version: Int,
                    id: AccountId,
                    currency: CurrencyUnit,
                    balance: Money) extends AggregateRoot[Account, AccountEvent, AccountId] {

  val log = Logger("Account")

  def markCommitted = copy(uncommittedEventsReverse = Nil)

  def loadedVersion(version: Int) = {
    copy(version = version)
  }

  def process(cmd: AccountCommand): Account = cmd match {
    case cmd: DepositMoney =>
      depositMoney(cmd.amount)
    case cmd: WithdrawMoney =>
      withdrawMoney(cmd.transactionId, cmd.amount)
  }


  def withdrawMoney(withdrawalId: TransactionId, amount: Money): Account = {
    ensureAmountIsPositive(amount)
    ensureSufficientFunds(amount, withdrawalId)
    ensureCurrenciesMatch(amount)

    applyEvent(MoneyWithdrawn(id, withdrawalId, amount)).subtractFromBalance(amount)
  }

  def depositMoney(amount: Money): Account = {
    ensureAmountIsPositive(amount)
    ensureCurrenciesMatch(amount)

    applyEvent(MoneyDeposited(id, amount)).addToBalance(amount)
  }

  private def addToBalance(amount: Money): Account = {
    applyEvent(BalanceChanged(id, balance+amount))
  }

  private def subtractFromBalance(amount: Money): Account = {
    applyEvent(BalanceChanged(id, balance-amount))
  }

  def ensureSufficientFunds(amount: Money, transactionId: TransactionId) {
    if (balance < amount) {
      throw new InsufficientFundsException("" + this + ". Not enough funds to place order " + transactionId + ". Amount requested: " + amount)
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
      case event: MoneyWithdrawn => when(event)
      case event: BalanceChanged => when(event)
      case event => throw new UnhandledEventException("Aggregate Account does not handle event " + event)
    }
  }

  def when(e: AccountOpened): Account = {
    Account(e :: uncommittedEventsReverse, 1, e.id, e.currency, Money(0, e.currency))
  }

  def when(e: MoneyDeposited): Account = {
    copy(uncommittedEventsReverse = e :: uncommittedEventsReverse)
  }

  def when(e: MoneyWithdrawn): Account = {
     copy(uncommittedEventsReverse = e :: uncommittedEventsReverse)
  }

  def when(e: BalanceChanged): Account = {
    copy(uncommittedEventsReverse = e :: uncommittedEventsReverse,
      balance=e.balance)
  }
}

