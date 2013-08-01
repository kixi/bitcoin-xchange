package domain

/**
 * Created with IntelliJ IDEA.
 * User: guenter
 * Date: 19.06.13
 * Time: 22:48
 * To change this template use File | Settings | File Templates.
 */
case class InsufficientFundsException(msg: String) extends RuntimeException(msg)
case class InvalidWithdrawalException(msg: String) extends RuntimeException(msg)

case class InvalidCurrencyException(msg: String) extends RuntimeException(msg)
case class UnhandledEventException(msg: String) extends RuntimeException(msg)
case class AggregateNotFoundException(msg: String) extends RuntimeException(msg)
case class OrderExpiredException(msg: String) extends RuntimeException(msg)
