package infra

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration.DurationInt
import scala.util.Random

object MyDispatcher extends App {

  class Counter extends Actor with ActorLogging {
    override def receive: Receive = count(0)

    def count(value: Int) : Receive = {
      case message: String =>
        log.info(s"[${self.path.name}] : [$value] : $message")
        context.become(count(value + 1))
    }
  }

  private val system: ActorSystem = ActorSystem("dispatchersDemo", ConfigFactory.load())

  /**
   * Method #1 : Programmatically Attaching Dispatcher to the Actor at the time of creation
   */
  private val actors: IndexedSeq[ActorRef] = for (i <- 1 to 10) yield system.actorOf(Props[Counter].withDispatcher("my-dispatcher"), s"counter_$i")
  val random = new Random
  //(1 to 1000).foreach ( id => actors(random.nextInt(10)) ! s"Sending message $id")

  /**
   * Method #2 : Attaching Dispatcher through configuration
   */
  private val system1: ActorSystem = ActorSystem("dispatchersDemo2", ConfigFactory.load().getConfig("dispatcherDemo"))
  private val counter: ActorRef = system1.actorOf(Props[Counter], "counter")
  (1 to 1000).foreach ( id => counter ! s"Sending message $id")

  system.scheduler.scheduleOnce(20 seconds){
    system.terminate()
    system1.terminate()
  }(system.dispatcher)

}