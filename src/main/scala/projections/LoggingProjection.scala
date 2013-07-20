package projections

import domain.{AccountOpened, MoneyDeposited, Event}
import com.weiglewilczek.slf4s.Logger
import akka.actor.Actor

/**
  * Created with IntelliJ IDEA.
  * User: guenter
  * Date: 20.06.13
  * Time: 13:56
  * To change this template use File | Settings | File Templates.
  */
class LoggingProjection extends Actor {
  val logger = Logger(classOf[LoggingProjection])
   def receive = {
     case e => System.out.printf(e.toString+"\n")
   }
  }
