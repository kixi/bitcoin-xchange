package domain.account

import domain._
import domain.CurrencyUnit
import domain.AccountId

import cqrs.{SpecTest, AggregateFactory, AggregateRoot}
import org.scalatest.FunSuite

class AccountTest extends SpecTest[Account, AccountFactory, AccountEvent] with FunSuite

class Account_T1 extends AccountTest {
  test("can request withdraw money if amount is equal to balance - new balance must be 0 ") {
    given(new AccountFactory()) {
      AccountOpened(AccountId("1"), CurrencyUnit("EUR"), new Money(0, CurrencyUnit("EUR"))) ::
        MoneyDeposited(AccountId("1"), new Money(100, CurrencyUnit("EUR"))) ::
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
        MoneyDeposited(AccountId("1"), new Money(100, CurrencyUnit("EUR"))) ::
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
        MoneyDeposited(AccountId("1"), new Money(100, CurrencyUnit("EUR"))) ::
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

      .requestMoneyWithdrawal(TransactionId("1"),Money(100, CurrencyUnit("EUR")))

    assert(account.uncommittedEventsReverse.head === MoneyWithdrawalRequested(AccountId("1"), TransactionId("1"),Money(100, CurrencyUnit("EUR")), Money(0, CurrencyUnit("EUR"))))
  }

  test("request withdrawal over limit") {
    intercept[InsufficientFundsException] {
      val account = new AccountFactory().create(new AccountId("1"), new CurrencyUnit("EUR"))
        .markCommitted

        .requestMoneyWithdrawal(TransactionId("1"),Money(100, CurrencyUnit("EUR")))
    }
  }

  test("confirm withdrawal ok") {
    val account = new AccountFactory().create(new AccountId("1"), new CurrencyUnit("EUR"))
      .depositMoney(Money(100, CurrencyUnit("EUR")))
      .requestMoneyWithdrawal(TransactionId("1"),Money(100, CurrencyUnit("EUR")))
      .markCommitted
      .confirmMoneyWithdrawal(TransactionId("1"))

    assert(account.uncommittedEventsReverse.head === MoneyWithdrawalConfirmed(AccountId("1"), TransactionId("1")))
  }

  test("confirm withdrawal - cannot confirm  same withdrawal twice") {
    intercept[InvalidWithdrawalException] {
      val account = new AccountFactory().create(new AccountId("1"), new CurrencyUnit("EUR"))
        .depositMoney(Money(100, CurrencyUnit("EUR")))
        .requestMoneyWithdrawal(TransactionId("1"),Money(100, CurrencyUnit("EUR")))
        .confirmMoneyWithdrawal(TransactionId("1"))
        .markCommitted
        .confirmMoneyWithdrawal(TransactionId("1"))
    }
  }

  test("confirm withdrawal - cannot confirm  a withdrawal which was never requested") {
    intercept[InvalidWithdrawalException] {
      val account = new AccountFactory().create(new AccountId("1"), new CurrencyUnit("EUR"))
        .depositMoney(Money(100, CurrencyUnit("EUR")))
        .requestMoneyWithdrawal(TransactionId("1"),Money(100, CurrencyUnit("EUR")))
        .confirmMoneyWithdrawal(TransactionId("1"))
        .markCommitted
        .confirmMoneyWithdrawal(TransactionId("2"))
    }
  }

}
