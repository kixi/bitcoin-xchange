package domain

/**
 * Created with IntelliJ IDEA.
 * User: guenter
 * Date: 19.06.13
 * Time: 22:07
 * To change this template use File | Settings | File Templates.
 */
case class Money(amount: BigDecimal, currency: CurrencyUnit) extends Ordered[Money] {
  def * (factor: BigDecimal) = {
    Money(amount*factor, currency)
  }

  def + (that: Money) = {
    Money(amount + that.amount, currency)
  }
  def - (that: Money) = {
    Money(amount - that.amount, currency)
  }

  def unary_! = {
    Money(0 - amount, currency)
  }
  override def compare(that: Money) = {
    (this.amount - that.amount).toBigInt().intValue()
  }
}
