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

package domain.account

import akka.actor.Actor
import domain._
import domain.OpenAccount
import domain.DepositMoney
import domain.AccountId

/**
 * Created with IntelliJ IDEA.
 * User: guenter
 * Date: 19.06.13
 * Time: 23:35
 * To change this template use File | Settings | File Templates.
 */
/*
class AccountAppService(val eventStore: EventStore) extends Actor with ApplicationService[Account, AccountId, AccountFactory, AccountEvent]{

  val factory = new AccountFactory()

  override def receive = {
    case c: OpenAccount => when(c)
    case c: DepositMoney => when(c)
    case c: RequestMoneyWithdrawal => when(c)
    case c: ConfirmMoneyWithdrawal => when(c)
  }

  def when(cmd: DepositMoney) {
    update(cmd.id, (a:Account) => a.depositMoney(cmd.amount))
  }

  def when(cmd: RequestMoneyWithdrawal) {
    update(cmd.id, (a:Account)=> a.requestMoneyWithdrawal(cmd.transactionId, cmd.amount))
  }

  def when(cmd: ConfirmMoneyWithdrawal) {
    update(cmd.id, (a:Account)=> a.confirmMoneyWithdrawal(cmd.transactionId))
  }

  def when(cmd: OpenAccount) {
    insert(cmd.id, (Unit) =>  factory.create(cmd.id, cmd.currency))
  }

 }
  */