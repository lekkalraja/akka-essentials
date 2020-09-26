package faulttolerance

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSelection, ActorSystem, Kill, PoisonPill, Props, Terminated}
import faulttolerance.StartingStoppingActor.SuperVisorActor.{StartActor, StopActor}

object StartingStoppingActor extends App {

  object SuperVisorActor {
    case class StartActor(name: String)
    case class StopActor(name: String)
  }



  class SuperVisorActor extends Actor with ActorLogging {
    override def receive: Receive = maintainChildren(Map())

    def maintainChildren(children: Map[String, ActorRef]): Receive = {
      case StartActor(name) =>
        log.info(s"Creating $name Actor")
        context.become(maintainChildren(children + (name -> context.actorOf(Props[SimpleActor], name))))
      case StopActor(name) =>
        log.info(s"Stopping $name Actor")
        children.get(name).foreach(actor => context.stop(actor)) // 1-WAY TO STOP ACTOR
    }
  }

  class SimpleActor extends Actor with ActorLogging {
    override def preStart(): Unit = log.info(s"[PRE-START-HOOK] ${context.self.path} Starting!")
    override def postStop(): Unit = log.info(s"[POST-STOP-HOOK] ${context.self.path} Stopped!")
    override def preRestart(reason: Throwable, message: Option[Any]): Unit = log.info(s"[PRE-RESTART-HOOK] ${context.self.path} Restarting with ${reason.getMessage}")
    override def postRestart(reason: Throwable): Unit = log.info(s"[POST-RESTART-HOOK] ${context.self.path} Restarted for ${reason.getMessage}")
    override def receive: Receive =  {
      case message => log.info(s"Got Simple Message $message")
    }
  }

  private val system: ActorSystem = ActorSystem("StartingStoppingActors")
  private val superVisionActor: ActorRef = system.actorOf(Props[SuperVisorActor], "SuperVisionActor")

  superVisionActor ! StartActor("Child1")

  // 1-WAY TO STOP(KILL) ACTOR (context.stop(ActorRef)
  superVisionActor ! StopActor("Child1")
  private val child: ActorSelection = system.actorSelection("/user/SuperVisionActor/Child1")

  for(i <- 1 to 50) child ! s"Sending Message ${i}"

  // ANOTHER-WAY TO STOP(KILL) ACTOR
  //child ! PoisonPill


  /**
   * ANOTHER-WAY TO STOP(KILL) ACTOR => IN THIS CASE IT WILL THROW
   * ERROR akka.actor.OneForOneStrategy - Kill
   *       akka.actor.ActorKilledException: Kill
   */
  //child ! Kill

  system.terminate()

}
