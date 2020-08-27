package actors

import actors.ActorsChangeBehavior.StatefulKid.{Accept, HAPPY, Reject, SAD}
import actors.ActorsChangeBehavior.Mom.{Ask, Chocolate, Food, Vegetable}
import actors.ActorsChangeBehavior.StatelessCounter.{Decrement, Increment, Print}
import actors.ActorsChangeBehavior.VoteAggregator.{AggregateVotes, Result}
import actors.ActorsChangeBehavior.Voter.{Vote, VoteStatusRequest, VoteStatusResponse}
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

  object StatelessCounter {
    case object Increment
    case object Decrement
    case object Print
  }

  class StatelessCounter extends Actor {
    override def receive: Receive = increment(0)

    def increment(counter: Int) : Receive = {
      case Increment => context.become(increment(counter+1))
      case Decrement => context.become(decrement(counter-1))
      case Print => println(s"Current Counter Value : $counter")
    }

    def decrement(counter: Int) : Receive = {
      case Increment =>  context.become(increment(counter+1))
      case Decrement =>  context.become(decrement(counter-1))
      case Print => println(s"Current Counter Value : $counter")
    }
  }

  private val counter: ActorRef = system.actorOf(Props[StatelessCounter], "counter")

  counter ! Decrement
  counter ! Print
  counter ! Increment
  counter ! Increment
  counter ! Print

  /**
   * Exercise : 2 # Simplified Voting system
   */

  object Voter {
    case class Vote(candidate: String)
    case object VoteStatusRequest
    case class VoteStatusResponse(candidate: Option[String])
  }

  class Voter extends Actor {

    override def receive: Receive = vote(None)

    def vote(candidate: Option[String]): Receive = {
      case Vote(c) => context.become(vote(Some(c)))
      case VoteStatusRequest => sender() ! VoteStatusResponse(candidate)
    }
  }

  object VoteAggregator {
    case class AggregateVotes(voters: Set[ActorRef])
    case object Result
  }

  class VoteAggregator extends Actor {
    override def receive: Receive = aggVotes(Map())

    def aggVotes(currentStatus: Map[String, Int]): Receive = {
      case AggregateVotes(voters) => voters.foreach(voter => voter ! VoteStatusRequest)
      case VoteStatusResponse(Some(candidate)) =>
        val votes = currentStatus.getOrElse(candidate, 0)
        context.become(aggVotes(currentStatus + (candidate -> (votes + 1))))
      case Result => println(currentStatus)
    }

  }

  val raj = system.actorOf(Props[Voter], "Raj")
  val achillies = system.actorOf(Props[Voter], "Achillies")
  val hector = system.actorOf(Props[Voter], "Hector")
  val helen = system.actorOf(Props[Voter], "Helen")
  val voteAggregator = system.actorOf(Props[VoteAggregator], "voteAggregator")

  raj ! Vote("YSJ")
  achillies ! Vote("YSJ")
  hector ! Vote("PK")
  helen ! Vote("CBN")

  voteAggregator ! AggregateVotes(Set(raj, achillies, hector, helen))
  Thread.sleep(100)
  voteAggregator ! Result


  system.terminate()
}