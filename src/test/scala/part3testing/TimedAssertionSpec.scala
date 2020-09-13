package part3testing

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike
import part3testing.TimedAssertionSpec.{WorkCompleted, Worker}

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

class TimedAssertionSpec extends TestKit(ActorSystem("TimedAssertions"))
                         with ImplicitSender
                         with AnyWordSpecLike
                         with BeforeAndAfterAll {

  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  "The Work Actor" should {

    val worker = system.actorOf(Props[Worker], "worker")

    "Receive Work in 500 to 1k millis" in {
      within(500 millis, 1 second) {
        worker ! "work"
        expectMsg(WorkCompleted(43))
      }
    }

    "Complete Work Seq" in {
      within(1 second) {
        worker ! "workSeq"
        val result = receiveWhile(2 seconds, 500 millis, 10) {
          case WorkCompleted(result) => result
        }
        assert(result.sum > 8)
      }
    }

    "With TestProbe Works As Expected" in {
      val probe = TestProbe()
      probe.send(worker, "work")
      probe.expectMsg(WorkCompleted(43))
    }

    "With TestProbe No Impact on within because probe will have default 3 seconds timeout" in {
      within(1 second) {
        val probe = TestProbe()
        probe.send(worker, "work")
        probe.expectMsg(WorkCompleted(43)) // Default timeout 3 seconds so no impact with within so override from application.conf
      }
    }
  }
}

object TimedAssertionSpec {

  case class WorkCompleted(result: Int)

  class Worker extends Actor {
    override def receive: Receive = {
      case "work" =>
        //LONG COMPUTATION
        Thread.sleep(500)
        sender() ! WorkCompleted(43)
      case "workSeq" =>
        (1 to 10) foreach { _ =>
          Thread.sleep(50)
          sender() ! WorkCompleted(1)
        }
    }
  }
}