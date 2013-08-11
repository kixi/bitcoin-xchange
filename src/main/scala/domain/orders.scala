/*
 * Copyright (c) 2013, Günter Kickinger.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * All advertising materials mentioning features or use of this software must
 * display the following acknowledgement: “This product includes software developed
 * by Günter Kickinger and his contributors.”
 * Neither the name of Günter Kickinger nor the names of its contributors may be
 * used to endorse or promote products derived from this software without specific
 * prior written permission.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS “AS IS”
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package domain

import org.joda.time.DateTime

/**
 * Created with IntelliJ IDEA.
 * User: guenter
 * Date: 19.06.13
 * Time: 22:06
 * To change this template use File | Settings | File Templates.
 */

trait Side

case object Buy extends Side

case object Sell extends Side

trait Order {
  def id: OrderId

  def product: CurrencyUnit

  def quantity: Quantity

  def creationDate: DateTime

  def side: Side

  def moneyAccount: AccountId

  def amount: Money

  def productAccount: AccountId

  def buy = (Buy == side)

  def sell = (Sell == side)

  def canExecute(order: Order): Boolean

  def hasHigherPriorityThan(order: Order): Boolean

  def expiryDate: DateTime = creationDate.plusDays(1)

  def expired = new DateTime().isAfter(expiryDate)
}

case class LimitOrder(
                       id: OrderId,
                       creationDate: DateTime = new DateTime,
                       product: CurrencyUnit,
                       quantity: Quantity,
                       limit: Money,
                       side: Side,
                       moneyAccount: AccountId,
                       productAccount: AccountId
                       ) extends Order {

  def canExecute(other: Order): Boolean = other match {
    case order: LimitOrder if this.buy && order.sell => this.limit >= order.limit
    case order: LimitOrder if this.sell && order.buy => this.limit <= order.limit
    case order: MarketOrder => true
    case _ => false
  }

  def hasHigherPriorityThan(other: Order): Boolean = {
    if (other.isInstanceOf[MarketOrder]) {
      this.creationDate.isBefore(other.creationDate)
    } else {
      val othLimit = other.asInstanceOf[LimitOrder]
      if (othLimit.buy && this.buy) {
        this.limit > othLimit.limit || (this.limit == othLimit.limit && this.creationDate.isBefore(othLimit.creationDate))
      } else if (othLimit.sell && this.sell) {
        this.limit < othLimit.limit || (this.limit == othLimit.limit && this.creationDate.isBefore(othLimit.creationDate))
      } else {
        false
      }
    }

  }

  def amount = limit * quantity
}

case class MarketOrder(
                        id: OrderId,
                        creationDate: DateTime = new DateTime,
                        product: CurrencyUnit,
                        quantity: Quantity,
                        price: Money,
                        side: Side,
                        moneyAccount: AccountId,
                        productAccount: AccountId
                        ) extends Order {

  def canExecute(other: Order): Boolean = {
    true
  }

  def hasHigherPriorityThan(other: Order): Boolean = {
    if (other.isInstanceOf[LimitOrder]) {
      true
    } else {
      this.creationDate.isBefore(other.creationDate)
    }
  }

  def amount = price * quantity
}

case class OrderBookList(buyOrders: List[Order], sellOrders: List[Order]) {
  def insert(order: Order) = {
    if (order.buy)
      copy(buyOrders=insertOrder(buyOrders, order))
    else
      copy(sellOrders=insertOrder(sellOrders, order))
  }

  def replaceHead(order: Order) = {
    if (order.buy)
      copy(buyOrders=order :: buyOrders.tail)
    else
      copy(sellOrders=order :: sellOrders.tail)
  }

  def removeHead(order: Order) = {
    if (order.buy)
      copy(buyOrders= buyOrders.tail)
    else
      copy(sellOrders= sellOrders.tail)
  }


  private def insertOrder(orderList: List[Order], order: Order): List[Order] = {
    if (orderList.isEmpty) order :: Nil
    else if (order.hasHigherPriorityThan(orderList.head)) {
      order :: orderList
    } else {
      orderList.head :: insertOrder(orderList.tail, order)
    }
  }

}
