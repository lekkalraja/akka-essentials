package actors

import actors.ActorsChangeBehavior.StatefulKid.{Accept, HAPPY, Reject, SAD}
import actors.ActorsChangeBehavior.Mom.{Ask, Chocolate, Food, Vegetable}
import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ActorsChangeBehavior extends App {


  private val system: ActorSystem = ActorSystem("ActorsChangeBehaviour")

  object StatefulKid {
    case object Accept
    case object Reject

    val HAPPY = "happy"
    val SAD = "sad"
  }

  object Mom {
    case class Food(message: AnyRef)
    case class Ask(message: String)

    case object Vegetable
    case object Chocolate
  }

  class StatefulKid extends Actor {
    var status: String = HAPPY

    override def receive: Receive = {
      case Vegetable =>
        println(s"[${self.path.name}] : Too sad, how can i eat Veggie :(")
        status = SAD
      case Chocolate =>
        println(s"[${self.path.name}] : Yay, Thanks mom for Chocolate ):")
        status = HAPPY
      case Ask(message) =>
        println(s"[${sender().path.name}] Asking : $message")
        if (status == HAPPY) sender() ! Accept
        else sender() ! Reject
    }
  }

  class StatelessKid extends Actor {

    override def receive: Receive = happyHandler

    def happyHandler : Receive = {
      case Vegetable =>
        println(s"[${self.path.name}] : Too sad, how can i eat Veggie :(")
        context.become(sadHandler, discardOld = false)
      case Chocolate =>
        println(s"[${self.path.name}] : Yay, Thanks mom for Chocolate ):")
      case Ask(message) =>
        println(s"[${sender().path.name}] Asking : $message")
        sender() ! Accept
    }

    def sadHandler : Receive = {
      case Vegetable =>
        println(s"[${self.path.name}] : Too sad, how can i eat Veggie :(")
      case Chocolate =>
        println(s"[${self.path.name}] : Yay, Thanks mom for Chocolate ):")
        context.become(happyHandler, discardOld = false)
      case Ask(message) =>
        println(s"[${sender().path.name}] Asking : $message")
        sender() ! Reject
    }
  }

  class Mom extends Actor {
    private val statefulKid: ActorRef = system.actorOf(Props[StatefulKid], "statefulKid")
    private val statelessKid: ActorRef = system.actorOf(Props[StatelessKid], "statelessKid")
    override def receive: Receive = {
      case Food(Vegetable) =>
        println(s"[${self.path.name}] sending Veggies to ${statelessKid.path.name}")
        statelessKid ! Vegetable
      case Food(Chocolate) =>
        println(s"[${self.path.name}] sending Chocolate to ${statelessKid.path.name}")
        statelessKid ! Chocolate
      case Ask(message) => statelessKid ! Ask(message)
      case Accept => println(s"[${self.path.name}] Yay, My Kid Accepted")
      case Reject => println(s"[${self.path.name}] My Kid sad :(, What Happened, baby!!")
    }
  }

  private val mom: ActorRef = system.actorOf(Props[Mom], "mom")

  mom ! Food(Vegetable)
  mom ! Ask("shall we go and play ?")
  /*mom ! Food(Chocolate)
  mom ! Ask("shall we go for movie ?")*/


  system.terminate()
}