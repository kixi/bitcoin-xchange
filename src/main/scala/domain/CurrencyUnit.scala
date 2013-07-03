package domain

/**
 * Created with IntelliJ IDEA.
 * User: guenter
 * Date: 19.06.13
 * Time: 22:08
 * To change this template use File | Settings | File Templates.
 */
case class CurrencyUnit(iso: String) {
  val isoCode = iso.trim().toUpperCase
}

