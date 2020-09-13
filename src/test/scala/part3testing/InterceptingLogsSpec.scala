package part3testing

import akka.actor.FSM.Event
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.testkit.{EventFilter, ImplicitSender, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike
import part3testing.InterceptingLogsSpec.{CheckoutActor, Order}

class InterceptingLogsSpec extends TestKit(ActorSystem("InterceptingLogsSpec"))
  with ImplicitSender with AnyWordSpecLike with BeforeAndAfterAll{

  override protected def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  "Checkout Order" should {
    val checkout = system.actorOf(Props[CheckoutActor], "checkout")
    val item = "Akka Essentials"
    val creditCard = "1234-4567-7890-0123"
    val invalidCreditCard = "0024-4567-7890-0123"

    "Dispatch" in {
      EventFilter.info(pattern = s"Order [0-9]+ of Items $item Dispatched", occurrences = 1) intercept {
        checkout ! Order(item, creditCard)
      }
    }

    "Throw RuntimeException for Invalid CC" in {
      EventFilter[RuntimeException](occurrences = 1) intercept {
        checkout ! Order(item, invalidCreditCard)
      }
    }
  }

}

object InterceptingLogsSpec {

  case class Order(item: String, card: String)
  case class Validate(card: String)
  case class Dispatch(item: String)

  case object PaymentAccepted
  case object PaymentDeclined


  class CheckoutActor extends Actor {
    private val payment: ActorRef = context.actorOf(Props[PaymentManager], "payment")
    private val orders: ActorRef = context.actorOf(Props[OrdersManager], "orders")

    override def receive: Receive = awaitingPayment

    def awaitingPayment : Receive = {
      case Order(item: String, card: String) =>  
        payment ! Validate(card)
        context.become(pendingDispatch(item))
    }

    def pendingDispatch(item: String): Receive = {
      case PaymentDeclined => throw new RuntimeException
      case PaymentAccepted => orders ! Dispatch(item)
        context.become(awaitingPayment)
    }
  }

  class PaymentManager extends Actor {
    override def receive: Receive = {
      case Validate(card) => if(card.startsWith("0")) sender() ! PaymentDeclined else sender() ! PaymentAccepted
    }
  }

  class OrdersManager extends Actor with ActorLogging{
    override def receive: Receive = dispatch(1)

    def dispatch(orderId: Int) : Receive = {
      case Dispatch(item) =>
        log.info(s"Order $orderId of Items $item Dispatched")
        context.become(dispatch(orderId+1))
    }
  }
}