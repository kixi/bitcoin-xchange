package projections

import domain.{AccountOpened, MoneyDeposited, Event}
import com.weiglewilczek.slf4s.Logger
import akka.actor.Actor
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

/**
  * Created with IntelliJ IDEA.
  * User: guenter
  * Date: 20.06.13
  * Time: 13:56
  * To change this template use File | Settings | File Templates.
  */
class LoggingProjection extends Actor {
  val logger = Logger(classOf[LoggingProjection])
  val fmt = DateTimeFormat.forPattern("HH:mm:ss:SSSS");
   def receive = {
     case e => System.out.printf(fmt.print(new DateTime()) +" "+ e.toString+"\n")
   }
  }
