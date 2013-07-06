package domain

/**
 * Created with IntelliJ IDEA.
 * User: guenter
 * Date: 19.06.13
 * Time: 21:53
 * To change this template use File | Settings | File Templates.
 */
trait Identity {
  def id: String
}

case class OrderBookId(id: String) extends Identity

case class AccountId(id: String) extends Identity

case class TransactionId(id: String) extends Identity

case class OrderId(id: String) extends Identity
object OrderId {
  def create = OrderId("1")
}
