package actors

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


  // terminate SimpleActorSystem
  simpleActorSystem.terminate()
}