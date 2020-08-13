package actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ActorsEntry extends App {

  // Create Actor System
  private val firstActorApp: ActorSystem = ActorSystem("FirstActorApp")

  //Define Actor
  class WordCounter extends Actor {
    //Actors Internal Data
    var totalWords = 0

    //Actors Behavior
    override def receive: PartialFunction[Any, Unit] = {
      case message: String => {
        totalWords = message.split(" ").length
        println(s"[word counter] : $message => $totalWords")
      }
      case msg => println(s"[word counter] INVALID MESSAGE ($msg)")
    }
  }

  //create actor
  private val wordCounter: ActorRef = firstActorApp.actorOf(Props[WordCounter], "wordCounter")

  //send message to word counter
  wordCounter ! "I am the First Actor in the world"
  wordCounter ! "I may reach before the first message because it's asynchronous"

  //can't instantiate Actor with new
  // new WordCounter // You cannot create an instance of [actors.ActorsEntry$WordCounter] explicitly using the constructor (new). You have to use one of the 'actorOf' factory methods to create a new actor. See the documentation.


  class Person(val name: String) extends Actor {
    override def receive: Receive = {
      case msg => println(s"I am $name")
    }
  }

  object Person {
    def props(name: String): Props = Props(new Person(name))
  }

  // private val personActor: ActorRef = firstActorApp.actorOf(Props(new Person("Raja")), "personActor") => Not a Good PRACTICE
  private val personActor: ActorRef = firstActorApp.actorOf(Person.props("Test"), "personActor")

  personActor ! "Hi"

  // terminate the ActorSystem
  firstActorApp.terminate()

}
