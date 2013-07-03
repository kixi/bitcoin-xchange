package domain

/**
 * Created with IntelliJ IDEA.
 * User: guenter
 * Date: 20.06.13
 * Time: 21:49
 * To change this template use File | Settings | File Templates.
 */
case class Balances(balances: Map[CurrencyUnit, Money]) {

  def this() = {
    this(Map.empty[CurrencyUnit, Money])
  }

  def + (that: Money) = {
    balances get that.currency match {
      case Some(balance) => Balances(balances + (that.currency -> (balance + that)))
      case None =>  Balances(balances + (that.currency -> that))
    }
  }

  def - (that: Money) = {
    this + (!that)
  }

  def apply(unit: CurrencyUnit) = {
    balances.getOrElse(unit, Money(0.0, unit))
  }
}
