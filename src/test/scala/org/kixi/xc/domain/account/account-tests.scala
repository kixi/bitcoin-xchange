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

package org.kixi.xc.domain.account

import org.scalatest.FunSuite
import org.kixi.xc.core.common._
import org.kixi.xc.core.account.domain._
import org.kixi.xc.core.account.domain.Account
import org.kixi.xc.core.account.domain.AccountId
import org.kixi.xc.core.common.InsufficientFundsException
import org.kixi.xc.core.common.InvalidWithdrawalException
import org.kixi.xc.core.account.domain.MoneyWithdrawalConfirmed
import org.kixi.xc.core.account.domain.MoneyWithdrawalRequested
import org.kixi.xc.core.account.domain.MoneyDeposited
import org.kixi.xc.core.account.domain.AccountOpened
import org.kixi.cqrslib.aggregate.SpecTest

class AccountTest extends SpecTest[Account, AccountFactory, AccountEvent, AccountId] with FunSuite

class Account_T1 extends AccountTest {
  test("can request withdraw money if amount is equal to balance - new balance must be 0 ") {
    given(new AccountFactory()) {
      AccountOpened(AccountId("1"), CurrencyUnit("EUR"), new Money(0, CurrencyUnit("EUR"))) ::
        MoneyDeposited(AccountId("1"), new Money(100, CurrencyUnit("EUR")), new Money(100, CurrencyUnit("EUR"))) ::
        Nil
    }

    when {
      (account: Account) => account.requestMoneyWithdrawal(TransactionId("1"), Money(100, CurrencyUnit("EUR")))
    }

    expected {
      MoneyWithdrawalRequested(AccountId("1"), TransactionId("1"), Money(100, CurrencyUnit("EUR")), Money(0, CurrencyUnit("EUR"))) ::
        Nil
    }
  }
}

class Account_T2 extends AccountTest {
  test("can request withdraw money if amount is smaller than balance - new balance is old balance - amount") {
    given(new AccountFactory()) {
      AccountOpened(AccountId("1"), CurrencyUnit("EUR"), new Money(0, CurrencyUnit("EUR"))) ::
        MoneyDeposited(AccountId("1"), new Money(100, CurrencyUnit("EUR")), new Money(100, CurrencyUnit("EUR"))) ::
        Nil
    }

    when {
      (account: Account) => account.requestMoneyWithdrawal(TransactionId("1"), Money(99, CurrencyUnit("EUR")))
    }

    expected {
      MoneyWithdrawalRequested(AccountId("1"), TransactionId("1"), Money(99, CurrencyUnit("EUR")), Money(1, CurrencyUnit("EUR"))) ::
        Nil
    }
  }
}

class Account_T3 extends AccountTest {
  test("cannot withdraw money if amount is larger than balance") {
    given(new AccountFactory()) {
      AccountOpened(AccountId("1"), CurrencyUnit("EUR"), new Money(0, CurrencyUnit("EUR"))) ::
        MoneyDeposited(AccountId("1"), new Money(100, CurrencyUnit("EUR")), new Money(100, CurrencyUnit("EUR"))) ::
        Nil
    }

    when {
      (account: Account) => account.requestMoneyWithdrawal(TransactionId("1"), Money(101, CurrencyUnit("EUR")))
    }

    expected {
      new InsufficientFundsException("")
    }
  }
}

class AccountTt extends FunSuite {
  test("request withdrawal ok") {
    val account = new AccountFactory().create(new AccountId("1"), new CurrencyUnit("EUR"))
      .depositMoney(Money(100, CurrencyUnit("EUR")))
      .markCommitted

      .requestMoneyWithdrawal(TransactionId("1"), Money(100, CurrencyUnit("EUR")))

    assert(account.uncommittedEventsReverse.head === MoneyWithdrawalRequested(AccountId("1"), TransactionId("1"), Money(100, CurrencyUnit("EUR")), Money(0, CurrencyUnit("EUR"))))
  }

  test("request withdrawal over limit") {
    intercept[InsufficientFundsException] {
      val account = new AccountFactory().create(new AccountId("1"), new CurrencyUnit("EUR"))
        .markCommitted

        .requestMoneyWithdrawal(TransactionId("1"), Money(100, CurrencyUnit("EUR")))
    }
  }

  test("confirm withdrawal ok") {
    val account = new AccountFactory().create(new AccountId("1"), new CurrencyUnit("EUR"))
      .depositMoney(Money(100, CurrencyUnit("EUR")))
      .requestMoneyWithdrawal(TransactionId("1"), Money(100, CurrencyUnit("EUR")))
      .markCommitted
      .confirmMoneyWithdrawal(TransactionId("1"))

    assert(account.uncommittedEventsReverse.head === MoneyWithdrawalConfirmed(AccountId("1"), TransactionId("1")))
  }

  test("confirm withdrawal - cannot confirm  same withdrawal twice") {
    intercept[InvalidWithdrawalException] {
      val account = new AccountFactory().create(new AccountId("1"), new CurrencyUnit("EUR"))
        .depositMoney(Money(100, CurrencyUnit("EUR")))
        .requestMoneyWithdrawal(TransactionId("1"), Money(100, CurrencyUnit("EUR")))
        .confirmMoneyWithdrawal(TransactionId("1"))
        .markCommitted
        .confirmMoneyWithdrawal(TransactionId("1"))
    }
  }

  test("confirm withdrawal - cannot confirm  a withdrawal which was never requested") {
    intercept[InvalidWithdrawalException] {
      val account = new AccountFactory().create(new AccountId("1"), new CurrencyUnit("EUR"))
        .depositMoney(Money(100, CurrencyUnit("EUR")))
        .requestMoneyWithdrawal(TransactionId("1"), Money(100, CurrencyUnit("EUR")))
        .confirmMoneyWithdrawal(TransactionId("1"))
        .markCommitted
        .confirmMoneyWithdrawal(TransactionId("2"))
    }
  }

}
