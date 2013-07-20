package domain.account

import org.scalatest.FunSuite
import domain._
import domain.CurrencyUnit
import domain.Money
import domain.TransactionId
import domain.AccountId

/**
 * Created with IntelliJ IDEA.
 * User: guenter
 * Date: 06.07.13
 * Time: 06:54
 * To change this template use File | Settings | File Templates.
 */
class AccountTest extends FunSuite {
  test("request withdrawal ok") {
    val account = new AccountFactory().create(new AccountId("1"), new CurrencyUnit("EUR"))
    .depositMoney(Money(100, CurrencyUnit("EUR")))
    .markCommitted

    .requestMoneyWithdrawal(TransactionId("1"),Money(100, CurrencyUnit("EUR")))

    assert(account.uncommittedEvents.head === MoneyWithdrawalRequested(AccountId("1"), TransactionId("1"),Money(100, CurrencyUnit("EUR")), Balances(Map(CurrencyUnit("EUR") -> Money(0, CurrencyUnit("EUR"))))))
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

    assert(account.uncommittedEvents.head === MoneyWithdrawalConfirmed(AccountId("1"), TransactionId("1")))
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
