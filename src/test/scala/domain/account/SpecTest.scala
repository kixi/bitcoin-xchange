package domain.account

import domain._
import domain.CurrencyUnit
import domain.Balances
import domain.AccountId
import domain.AccountOpened

/**
 * Created with IntelliJ IDEA.
 * User: guenter
 * Date: 19.07.13
 * Time: 23:28
 * To change this template use File | Settings | File Templates.
 */
class SpecTest {
  var events : List[Event] = _
  def given(f: => List[Event]) {
    events =  f
  }

  def when (cmd: Command) {

  }

  def then = {

  }
}


class SomeTest extends SpecTest {
  given {
    AccountOpened(AccountId("1"), CurrencyUnit("EUR"), new Balances()) :: Nil
  }

  when {
    RequestMoneyWithdrawal(AccountId("1"), TransactionId("1"), new Money(100, CurrencyUnit("EUR")))
  }

  then
}