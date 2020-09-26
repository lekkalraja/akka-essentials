package infra

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props, Terminated}
import akka.routing.{ActorRefRoutee, FromConfig, RoundRobinRoutingLogic, Router}
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

object MyRouters extends App {

  class Worker extends Actor with ActorLogging {
    override def receive: Receive = {
      case message : String => log.info(s"[${self.path.name}] Received message : $message")
    }
  }

  /**
   * Method #1 : Creating Router manually
   */

  class MyRouter extends Actor with ActorLogging {
    // Step 1 : Create workers and make them routee
    private val routees: IndexedSeq[ActorRefRoutee] = (1 to 5).map { i =>
      val worker = context.actorOf(Props[Worker], s"Worker_$i")
      context.watch(worker)
      ActorRefRoutee(worker)
    }

    // Step 2 : Define Router
    private val router: Router = Router(RoundRobinRoutingLogic(), routees)

    override def receive: Receive = routeManager(router)

    def routeManager(router : Router) : Receive = {
        // Step 3 : Route the messages
      case message => router.route(message, sender())
        // Step 4 : Handle the termination/lifecycle of the routees
      case Terminated(ref) =>
        val router1 = router.removeRoutee(ref)
        val newWorker = context.actorOf(Props[Worker], "Worker_Rescue")
        context.watch(newWorker)
        context.become(routeManager(router1.addRoutee(newWorker)))
    }
  }

  /*private val system: ActorSystem = ActorSystem("MyRouters")
  private val myRouter: ActorRef = system.actorOf(Props[MyRouter], "myRouter")
  (1 to 10).foreach(id => myRouter ! s"I am Manual Router $id")*/

  /**
   * Method 2.1 (POOL ROUTER) # Create POOL of Workers as it's children programmatically
   */

  /*private val system: ActorSystem = ActorSystem("ProgrammaticPoolSystem")
  private val poolRouter: ActorRef = system.actorOf(RoundRobinPool(5).props(Props[Worker]), "poolRouter")
  (1 to 10).foreach(id => poolRouter ! s"I am Programmatic Pool Actor $id")*/

  /**
   * Method 2.2 (POOL ROUTER) # Create POOL Of Workers by specifying config's in file (application.conf)
   */
  /*private val system: ActorSystem = ActorSystem("ConfigPoolRouter", ConfigFactory.load().getConfig("routerDemo"))
  private val configPoolRouter: ActorRef = system.actorOf(FromConfig.props(Props[Worker]), "configPoolRouter")
  (1 to 10).foreach(id => configPoolRouter ! s"I am Config Pool Router $id")*/

  /**
   * Method 3.1 (Group ROUTER) # Grouping Actors(may be created anywhere in the app) and making them as a pool
   */

  /*private val system: ActorSystem = ActorSystem("GroupRouter")
  // Collect Actors Path as a List
  private val actorsPath: IndexedSeq[String] = (1 to 5).map(id => system.actorOf(Props[Worker], s"Worker_$id").path.toString)
  private val groupRouter: ActorRef = system.actorOf(RoundRobinGroup(actorsPath).props())
  (1 to 10).foreach(id => groupRouter ! s"I am Group Router $id")*/

 private val system: ActorSystem = ActorSystem("ConfigGroupRouter", ConfigFactory.load().getConfig("routerDemo"))
 (1 to 5).map(id => system.actorOf(Props[Worker], s"Worker_$id"))
 private val configGroupRouter: ActorRef = system.actorOf(FromConfig.props(), "configGroupRouter")
 (1 to 10).foreach(id => configGroupRouter ! s"I am Config Group Router $id")

  system.scheduler.scheduleOnce(7 seconds){
    system.terminate()
  }(system.dispatcher)
}
