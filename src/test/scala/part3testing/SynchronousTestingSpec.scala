package part3testing

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{TestActorRef, TestProbe}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike
import part3testing.SynchronousTestingSpec.{Counter, Inc, Read}

import scala.concurrent.duration.Duration

class SynchronousTestingSpec extends AnyWordSpecLike with BeforeAndAfterAll {

  implicit private val system: ActorSystem = ActorSystem("SynchronousTestingSpec")

  override protected def afterAll(): Unit = system.terminate()

  "A Counter" should {

    "synchronously increase it's counter" in {
      val counter = TestActorRef[Counter](Props[Counter])
      counter ! Inc

      assert( counter.underlyingActor.count == 1)
    }

    "synchronously increase it's counter at the call of the receive function" in {
      val counter = TestActorRef[Counter]
      counter.receive(Inc)

      assert( counter.underlyingActor.count == 1)
    }

    "work on the calling thread dispatcher" in {
      val counter = system.actorOf(Props[Counter])
      val probe = TestProbe()

      probe.send(counter, Read)
      probe.expectMsg(0)// probe has Already received the message 0
    }
  }

}

object SynchronousTestingSpec {

  case object Inc
  case object Read

  class Counter extends Actor {
    var count = 0
    override def receive: Receive = operate

    def operate : Receive = {
      case Inc => count += 1//context.become(operate(count+1))
      case Read => sender() ! count
    }
  }
}