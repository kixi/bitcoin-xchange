package domain.orderbook

import org.scalatest.FunSuite
import domain._
import domain.Money
import domain.OrderPlacementPrepared
import domain.OrderBookId
import domain.CurrencyUnit
import domain.OrderPlaced
import domain.LimitOrder
import domain.OrderPlacementConfirmed
import domain.TransactionId
import domain.AccountId

/**
 * Created with IntelliJ IDEA.
 * User: guenter
 * Date: 06.07.13
 * Time: 06:22
 * To change this template use File | Settings | File Templates.
 */
class OrderBookTest extends FunSuite {
  test("initiate place order") {
    val book = new OrderBookFactory().create(OrderBookId("EURBTC"))
    .markCommitted
    .placeOrder(TransactionId("1"), LimitOrder(CurrencyUnit("BTC"), 5.0, Money(100, CurrencyUnit("EUR")), AccountId("1")))

    assert(book.uncommittedEvents.head === OrderPlaced(OrderBookId("EURBTC"), TransactionId("1"),  LimitOrder(CurrencyUnit("BTC"), 5.0, Money(100, CurrencyUnit("EUR")), AccountId("1"))))
  }

  test("prepare an order - ok") {
    val book = new OrderBookFactory().create(OrderBookId("EURBTC"))
      .markCommitted
      .prepareOrderPlacement(OrderId("1"),TransactionId("1"),  LimitOrder(CurrencyUnit("BTC"), 5.0, Money(100, CurrencyUnit("EUR")), AccountId("1")))

    assert(book.uncommittedEvents.head === OrderPlacementPrepared(OrderBookId("EURBTC"), TransactionId("1"), OrderId("1"), LimitOrder(CurrencyUnit("BTC"), 5.0, Money(100, CurrencyUnit("EUR")), AccountId("1"))))
  }

  test("confirm an order - ok") {
    val book = new OrderBookFactory().create(OrderBookId("EURBTC"))
      .prepareOrderPlacement(OrderId("1"),TransactionId("1"),  LimitOrder(CurrencyUnit("BTC"), 5.0, Money(100, CurrencyUnit("EUR")), AccountId("1")))
      .markCommitted
      .confirmOrderPlacement(OrderId("1"), TransactionId("1") )

    assert(book.uncommittedEvents.head === OrderPlacementConfirmed(OrderBookId("EURBTC"), TransactionId("1"), OrderId("1")))
  }

}
