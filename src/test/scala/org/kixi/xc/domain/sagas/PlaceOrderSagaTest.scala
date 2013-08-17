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

package domain.sagas


/**
 * User: guenter
 * Date: 06.07.13
 * Time: 10:59
 */
/*
class CommandReceiverMock extends Actor {
  var receivedCommands = List.empty[Command]
  def receive = {
    case e: Command => receivedCommands = e :: receivedCommands
  }
}

class PlaceOrderSagaTest extends TestKit(ActorSystem("test")) with FunSuite {
  test("x") {
    val commandDispatcher = TestActorRef[CommandReceiverMock]
 //   val actorRef = system.actorOf(Props(new PlaceOrderSaga(commandDispatcher) ))
    val fsm = TestFSMRef(new PlaceOrderSaga(commandDispatcher))

    assert(fsm.stateName === Start)
    assert(fsm.stateData === Uninitialized)

    fsm ! OrderPlaced(OrderBookId("1"), TransactionId("2"), LimitOrder("1", new DateTime(), CurrencyUnit("BTC"), 5.0, Money(100, CurrencyUnit("EUR")), "B", AccountId("3"), AccountId("1")))

    assert(commandDispatcher.underlyingActor.receivedCommands.head === RequestMoneyWithdrawal(AccountId("3"), TransactionId("2"), Money(500, CurrencyUnit("EUR"))))
    fsm ! MoneyWithdrawalRequested(AccountId("3"), TransactionId("2"),Money(500, CurrencyUnit("EUR")), Money(0, CurrencyUnit("EUR")))

    assert(commandDispatcher.underlyingActor.receivedCommands.head === PrepareOrderPlacement(OrderBookId("1"), TransactionId("2"), OrderId("1"), LimitOrder("1", new DateTime(), CurrencyUnit("BTC"), 5.0, Money(100, CurrencyUnit("EUR")), "B", AccountId("1"), AccountId("3"))))

  }

}
    */