package domain

/**
 * Created with IntelliJ IDEA.
 * User: guenter
 * Date: 19.06.13
 * Time: 22:06
 * To change this template use File | Settings | File Templates.
 */


trait OrderInfo
case class LimitOrder (val product: CurrencyUnit,val quantity: Quantity, val pricePerUnit: Money, val account: AccountId) extends OrderInfo {
  def amount = pricePerUnit * quantity
}
