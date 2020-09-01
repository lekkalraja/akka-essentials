package actors

import actors.ActorsChild.Parent.{CreateChild, TellChild}
import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ActorsChild extends App {

  object Parent {
    case class CreateChild(name: String)
    case class TellChild(name: String, message: String)
  }

  class Parent extends Actor {
    override def receive: Receive = {
      case CreateChild(name) => context.actorOf(Props[Child], name)
      case TellChild(name, message) => context.actorSelection(s"${self.path}/$name") forward message
    }
  }


  /*class Parent extends Actor {
    override def receive: Receive = {
      case CreateChild(name) =>
        val childRef = context.actorOf(Props[Child], name)
        context.become(tellChild(childRef))
    }

    def tellChild(childRef: ActorRef) : Receive = {
      case TellChild(_,message) => childRef forward message
        context.become(receive)
    }
  }*/


  class Child extends Actor {
    override def receive: Receive = {
      case message => println(s"${self.path} I got Message: $message")
    }
  }

  private val system: ActorSystem = ActorSystem("ParentChild")
  private val parent: ActorRef = system.actorOf(Props[Parent], "parent")

  parent ! CreateChild("child")
  parent ! TellChild("child", "Hey, Where are You ?")
  parent ! CreateChild("child1")
  parent ! TellChild("child1", "Hey, when you are going to come ?")

  //Actors Selection
  system.actorSelection("/user/parent/child") ! "I Bypassed your parent !!!"

  system.terminate()

  /**
   * 3 ROOT LEVEL GUARDIAN ACTORS
   *  /  -> ROOT ACTOR
   *  /system -> TAKE CARE OF SYSTEM RELATED ACTORS
   *  /user -> TAKE CARE (SUPERVISE) OF OUR ACTORS
   */
}