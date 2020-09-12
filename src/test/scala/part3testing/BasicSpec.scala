package part3testing

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalactic.source.Position
import org.scalatest.BeforeAndAfter
import org.scalatest.wordspec.AnyWordSpecLike
import part3testing.BasicSpec.{BlackHoleActor, EchoActor, LabTestActor}

import scala.language.postfixOps
import scala.concurrent.duration.DurationInt
import scala.util.Random

class BasicSpec extends TestKit(ActorSystem("BasicSpec"))
                with ImplicitSender
                with AnyWordSpecLike
                with BeforeAndAfter{

  override def after(fun: => Any)(implicit pos: Position): Unit = {
    system.terminate()
  }

  "An Echo Actor" should {
    val echoActor = system.actorOf(Props[EchoActor], "echoActor")

    "echo message" in {
      echoActor ! "Replicate"
      expectMsg("Replicate")
    }

    "convert received message" in {
      echoActor ! "UpperCase"
      val received = expectMsgType[String]
      assert( received == "UpperCase")
    }
  }

  "A BlackHole Actor" should {
    val blackHole = system.actorOf(Props[BlackHoleActor], "blackHole")

    "not receive message in 1 sec" in {
      blackHole ! "Gone"
      expectNoMessage(1 second)
    }

    "should get timeout " in {
      blackHole ! "Gone"

      //expectMsg("Test") // assertion failed: timeout (3 seconds) during expectMsg while waiting for Test
    }
  }

  "A Lab Test Actor" should {
    val labTest = system.actorOf(Props[LabTestActor], "labTest")

    "May send Hi or Hello" in {
      labTest ! "greet"
      expectMsgAnyOf("Hi", "Hello")
    }

    "Send Scala & Akka" in {
      labTest ! "favourite"
      expectMsgAllOf("Scala", "Akka")
    }

    "Send 2 Messages" in {
      labTest ! "favourite"
      val messages = receiveN(2)
      assertResult(2)(messages.length)
    }

    "Partial Function based on reply" in {
      labTest ! "random"

      expectMsgPF() {
        case "Scala" =>
        case "Akka"  =>
        case "Spark" =>
      }
    }
  }
}

object BasicSpec {

  class EchoActor extends Actor {
    override def receive: Receive = {
      case message => sender() ! message
    }
  }

  class BlackHoleActor extends Actor {
    override def receive: Receive = Actor.emptyBehavior
  }

  class LabTestActor extends Actor {
    val random = new Random()
    override def receive: Receive = {
      case "greet" => if (random.nextBoolean()) sender() ! "Hi" else sender() ! "Hello"
      case "favourite" =>
        sender() ! "Scala"
        sender() ! "Akka"
      case "random" => sender() ! "Spark"
    }
  }
}