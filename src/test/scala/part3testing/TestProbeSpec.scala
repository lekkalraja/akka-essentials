package part3testing

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike
import part3testing.TestProbeSpec.{Master, RegisterAck, RegisterSlave, Report, SlaveWork, Work, WorkCompleted}


/**   SCENARIO
 * Word Counting Actor Hierarchy Master-Slave
 *  - send some work to the master
 *      1. Master sends the slave the piece of work
 *      2. Slave Processes the work and replies to master
 *      3. Master aggregates the result
 *    master sends the total count to the original requester
 */

class TestProbeSpec extends TestKit(ActorSystem("TestProbeSpec"))
                    with ImplicitSender
                    with AnyWordSpecLike
                    with BeforeAndAfterAll {
  override def afterAll(): Unit = system.terminate()

  "Master Slave Architecture" should {

    val master = system.actorOf(Props[Master], "master1")
    val slave = TestProbe("slave")

    "Register slave" in {
      master ! RegisterSlave(slave.ref)
      expectMsg(RegisterAck)
    }

    "Send work to Slave" in {
      val work = "I Love Akka"
      master ! Work(work)
      slave.expectMsg(SlaveWork(work, testActor))
    }

    "Get Reply from slave" in {
      val work = "I Love Akka"
      master ! Work(work)
      slave.expectMsg(SlaveWork(work, testActor))
      slave.reply(WorkCompleted(3, testActor))
      expectMsg(Report(3))
    }

    "Maintain count's stateful " in {
      val master2= system.actorOf(Props[Master], "master2")
      master2 ! RegisterSlave(slave.ref)
      //expectMsg(RegisterAck)
      val work = "I Love Akka"
      master2 ! Work(work)
      master2 ! Work(work)
      slave.receiveWhile() {
        case SlaveWork(`work`, `testActor`) => slave.reply(WorkCompleted(3, testActor))
      }
      //expectMsg(Report(3)) // Above Test case generates 3
      //expectMsg(Report(6))
      expectMsgAllOf(RegisterAck, Report(3), Report(6))
    }
  }
}

object TestProbeSpec {

  case object RegisterAck
  case class RegisterSlave(slaveRef: ActorRef)
  case class Work(message: String)
  case class SlaveWork(message: String, originalSender: ActorRef)
  case class WorkCompleted(count: Int, originalRequester: ActorRef)
  case class Report(count: Int)

  class Master extends Actor {

    def online(slaveRef: ActorRef, totalCount: Int): Receive = {
      case Work(message) => slaveRef ! SlaveWork(message, sender())
      case WorkCompleted(count, originalRequester) =>
        val newCount = totalCount + count
        originalRequester ! Report(newCount)
        context.become(online(slaveRef, newCount))
    }


    override def receive: Receive = {
      case RegisterSlave(slaveRef) =>
        sender() ! RegisterAck
        context.become(online(slaveRef, 0))
      case _ => // Ignore for time-being
    }
  }
}