package patterns

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props, Stash}
import patterns.StashDemo.ResourceActor.{Close, Open, Read, Write}

import scala.concurrent.duration.DurationInt

object StashDemo extends App {

  /**
   * ResourceActor:
   *  - Open => It can Receive Read/Write requests to the resource
   *  - otherwise it will postpone all Read/Write requests until the state is open
   *
   *  ResourceActor is Closed:
   *    - Open => Switch to the open state
   *    - Read, Write messages are POSTPONED
   *
   *  ResourceActor is Open
   *    - Read, Write are handled
   *    - Close => Switch to the closed state
   *
   *  [Open, Read, Read, Write] (Assuming Resource is in Closed State)
   *    - Switch to the open state
   *    - read the data
   *    - read the data
   *    - write the data
   *
   *  [Read, Open, Write] (Assuming Resource is in Closed State)
   *    - stash Read
   *      Stash: [Read]
   *    - Open => Switch to the Open state
   *      Mailbox: [Read, Write]
   *    - read and write are handled
   */

  object ResourceActor {
    case object Open
    case object Close
    case object Read
    case class Write(data: String)
  }
  class ResourceActor extends Actor with ActorLogging with Stash {
    private var innerData = ""
    override def receive: Receive = close

    def close: Receive = {
      case Open =>
        log.info("Switching to Open State")
        unstashAll()
        context.become(open)
      case message =>
        log.info(s"Stashing $message, because i can't handle in Closed State")
        stash()

    }

    def open : Receive = {
      case Close =>
        log.info("Switching to Close State")
        unstashAll()
        context.become(close)
      case Read => log.info(s"Reading Resource Data $innerData")
      case Write(message) =>
        log.info(s"Writing $message to Resource")
        innerData = message
      case message =>
        log.info(s"Stashing $message, because i can't handle in Open State")
        stash()
       // stash() // ERROR akka.actor.OneForOneStrategy - Can't stash the same message Envelope(Open,Actor[akka://StashDemo/deadLetters]) more than once
    }
  }

  private val system: ActorSystem = ActorSystem("StashDemo")
  private val resourceActor: ActorRef = system.actorOf(Props[ResourceActor], "resourceActor")

  resourceActor ! Read
  resourceActor ! Open
  resourceActor ! Open
  resourceActor ! Write("I Love Stash")
  resourceActor ! Close
  resourceActor ! Read

  /**
   * Things to be careful about
   *  1. Potential memory bounds on Stash
   *  2. Potential mailbox bounds when unstashing
   *  3. no stashing twice
   *  4. the Stash trait overrides preRestart so must be mixed-in last
   */

  system.scheduler.scheduleOnce(10 seconds){
    system.terminate()
  }(system.dispatcher)

}