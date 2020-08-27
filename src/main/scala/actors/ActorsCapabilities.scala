package actors

import actors.ActorsCapabilities.BankAccount.{Deposit, Statement, TxnFailure, TxnSuccess, Withdraw}
import actors.ActorsCapabilities.Counter.{Decrement, Increment, Print}
import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ActorsCapabilities extends App {

  // ! => tell

  class SimpleActor extends Actor {
    override def receive: Receive = {
      case message : String => println(s"[${context.self.path.name}] I have Received String :: $message")
      case number: Int => println(s"[${self.path.name}] I have Received Number :: $number" )
      case SimpleMessage(content) => println(s"[${self.path.name}] I have Received Complex Object with Content :: $content")
      case Greet(message, ref) =>
        println(s"[${self.path.name.toUpperCase()}] saying :: $message")
        ref ! GreetReply("I am Good, How about you ")
      case GreetReply(greet) => println(s"[${self.path.name}] Replying :: $greet ${sender().path.name}!!")
      case Forward(message, ref) =>
        println(s"[${sender().path.name}] Asking ${self.path.name} forward the message to ${ref.path.name}")
        ref forward GreetReply(message) // forward can hold the original sender
    }
  }


  private val simpleActorSystem: ActorSystem = ActorSystem("SimpleActorSystem")
  private val juniorActor: ActorRef = simpleActorSystem.actorOf(Props[SimpleActor], "juniorActor")

  // Can send any type of messages
  juniorActor ! "Hello, String!!!"
  juniorActor ! 432

  case class SimpleMessage(content: String)
  juniorActor ! SimpleMessage("I am Simple")

  case class Greet(message: String, ref: ActorRef)
  case class GreetReply(message: String)
  case class Forward(message: String, ref: ActorRef)

  private val achillies: ActorRef = simpleActorSystem.actorOf(Props[SimpleActor], "Achillies")
  private val hector: ActorRef = simpleActorSystem.actorOf(Props[SimpleActor], "Hector")

  achillies ! Greet("Hi, How are you ?", hector)

  // nosender(deadLetters) -> achillies -> hector
  achillies ! Forward("This message was sent by", hector)


  /**
   * Exercise :
   *  1. Counter Actor
   *    - Increment
   *    - Decrement
   *    - Print
   */

  object Counter {
    case object Increment
    case object Decrement
    case object Print
  }

  class Counter extends Actor {
    private val counter: Int = 0

    override def receive: Receive = onMessage(counter)

    private def onMessage(counter: Int): Receive = {
      case Increment => context.become(onMessage(counter + 1))
      case Decrement => context.become(onMessage(counter - 1))
      case Print => println(s"Current Counter value is :: $counter")
    }
  }

  private val counter: ActorRef = simpleActorSystem.actorOf(Props[Counter], "counter")

  (1 to 5).foreach(_ => counter ! Increment)
  (1 to 3).foreach(_ => counter ! Decrement)
  counter ! Print

  /**
   * Exercise:
   *  2. a Bank Account as an Actor
   *   - Receives
   *     - Deposit an amount
   *     - Withdraw amount
   *     - Statement
   *   - Replies (interact with some other kind of Actor)
   *     - Success
   *     - Failure
   */

  object BankAccount {
    case class Deposit(amount: Double)
    case class Withdraw(amount: Double)
    case object Statement

    case class TxnSuccess(message: String)
    case class TxnFailure(reason: String)
  }

  class BankAccount extends Actor {
    var funds : Double = 0

    override def receive: Receive = {
      case Deposit(amount) =>
        if (amount < 0) sender() ! TxnFailure(s"Got Invalid Deposited amount : $amount")
        else {
          funds += amount
          sender() ! TxnSuccess(s"Successfully Deposited amount : $amount")
        }
      case Withdraw(amount) =>
        if(amount < 0) sender() ! TxnFailure(s"Got Invalid Withdraw amount : $amount")
        else if (amount > funds) sender() ! TxnFailure(s"Got Withdraw amount $amount more than actual funds $funds")
        else {
          funds -= amount
          sender() ! TxnSuccess(s"Successfully Withdrawn amount : $amount")
        }
      case Statement => sender() ! s"Currently the available Balance is $funds"
    }
  }

  case object LiveTheLife

  class Person extends Actor {
    private val bankAccount: ActorRef = simpleActorSystem.actorOf(Props[BankAccount], "bankAccount")

    override def receive: Receive = {
      case LiveTheLife =>
        bankAccount ! Deposit(10000)
        bankAccount ! Deposit(-1000)
        bankAccount ! Withdraw(20000)
        bankAccount ! Withdraw(400)
        bankAccount ! Statement
      case message => println(message.toString)
    }
  }

  private val person: ActorRef = simpleActorSystem.actorOf(Props[Person], "person")
  person ! LiveTheLife

  // terminate SimpleActorSystem
  simpleActorSystem.terminate()
}