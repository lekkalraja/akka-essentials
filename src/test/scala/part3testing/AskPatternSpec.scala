package part3testing

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike
import patterns.AuthManager
import patterns.AuthManager._

class AskPatternSpec extends TestKit(ActorSystem("AskPattern"))
  with ImplicitSender with AnyWordSpecLike with BeforeAndAfterAll{

  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  "An Authenticator" should {

    val authManager = system.actorOf(Props[AuthManager], "authManager")

    "User doesn't exist" in {
      authManager ! AuthenticateUser("Raja", "kvd")
      expectMsg(AuthFailure(AUTH_FAILURE_USER_NOT_FOUND))
    }

    "Provided Password is Incorrect" in  {
      authManager ! RegisterUser("Raja", "kvd")
      authManager ! AuthenticateUser("Raja", "kvd1")
      expectMsg(AuthFailure(AUTH_FAILURE_PASSWORD_INCORRECT))
    }

    "Success Authentication" in {
      authManager ! AuthenticateUser("Raja", "kvd")
      expectMsg(AuthSuccess)
    }
  }
}