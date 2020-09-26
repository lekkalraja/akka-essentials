package infra

import akka.actor.ActorSystem.Settings
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.dispatch.{ControlMessage, PriorityGenerator, UnboundedPriorityMailbox}
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.duration.DurationInt

object MyMailbox extends App {

  class SimpleActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case message  => log.info(s"[${self.path.name}] Received message ${message.toString}")
    }
  }

  /**
   * Interesting case #1 : Custom Priority Mailbox
   * P0 -> Most Important
   * P1
   * P2
   * P3 -> Lowest Important
   */

  // STEP #1 : Mailbox Definition
  class SupportTicketPriorityMailbox(settings: Settings, config: Config)
    extends UnboundedPriorityMailbox(
      PriorityGenerator {
        case message: String if message.startsWith("[P0]") => 0
        case message: String if message.startsWith("[P1]") => 1
        case message: String if message.startsWith("[P2]") => 2
        case message: String if message.startsWith("[P3]") => 3
        case _ => 4
      }
    )

  // STEP #2: Make it known in the config
  private val system: ActorSystem = ActorSystem("MailboxDemo", ConfigFactory.load().getConfig("mailboxDemo"))
  private val mailboxActor: ActorRef = system.actorOf(Props[SimpleActor].withMailbox("support-ticket-dispatcher"), "priorityMailBox")

  mailboxActor ! "[P5] Lowest Important"
  mailboxActor ! "[P3] Little Important"
  mailboxActor ! "[P2] Little More Important"
  mailboxActor ! "[P0] Very Very Important"
  mailboxActor ! "[P1] Important"

  /**
   * OUTPUT:::
   * 22:44:23.212 [MailboxDemo-akka.actor.default-dispatcher-6] INFO  infra.MyMailbox$SimpleActor - Received message [P0] Very Very Important
   * 22:44:23.216 [MailboxDemo-akka.actor.default-dispatcher-6] INFO  infra.MyMailbox$SimpleActor - Received message [P1] Important
   * 22:44:23.216 [MailboxDemo-akka.actor.default-dispatcher-6] INFO  infra.MyMailbox$SimpleActor - Received message [P2] Little More Important
   * 22:44:23.216 [MailboxDemo-akka.actor.default-dispatcher-6] INFO  infra.MyMailbox$SimpleActor - Received message [P3] Little Important
   * 22:44:23.216 [MailboxDemo-akka.actor.default-dispatcher-6] INFO  infra.MyMailbox$SimpleActor - Received message [P5] Lowest Important
   */

  /**
   * Interesting case #2: Control-aware mailbox
   * we'll use UnboundedControlAwareMailbox
   */

  // Step #1: Mark important messages as control messages
  case object VeryImportant extends ControlMessage

  // Step #2: Configure who gets the mail box. make the actor attach to the mailbox

  private val controlActor: ActorRef = system.actorOf(Props[SimpleActor].withMailbox("control-mailbox"), "controlMailbox")
  controlActor ! "[P0] Very Very Important"
  controlActor ! "[P1] Important"
  controlActor ! VeryImportant

  system.scheduler.scheduleOnce(5 seconds) {
    system.terminate()
  }(system.dispatcher)

}
