package domain

import java.util.UUID

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

case class AccountId(id: String = UUID.randomUUID().toString) extends Identity

case class TransactionId(id: String = UUID.randomUUID().toString) extends Identity

case class OrderId(id: String = UUID.randomUUID().toString) extends Identity

