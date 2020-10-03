package patterns

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import patterns.AuthManager._
import patterns.DBActor.{Read, Write}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContextExecutor, Future}

object AuthManager {
  case class RegisterUser(username: String, password: String)
  case class AuthenticateUser(username: String, password: String)
  case class AuthFailure(message: String)
  case object AuthSuccess
  val AUTH_FAILURE_USER_NOT_FOUND = "User Not Found"
  val AUTH_FAILURE_PASSWORD_INCORRECT = "Password Incorrect"
}

class AuthManager extends Actor with ActorLogging {

  private val dbActor: ActorRef = context.actorOf(Props[DBActor], "dbActor")
  implicit private val timeout: Timeout = Timeout(1 second)
  implicit val dispatcher: ExecutionContextExecutor = context.dispatcher

  override def receive: Receive = {
    case RegisterUser(username, password) => dbActor ! Write(username, password)
    case AuthenticateUser(username, password) =>
      log.info(s"Authenticating for $username")
      val future : Future[Any]= dbActor ? Read(username)
      val passwordResp : Future[Option[String]] = future.mapTo[Option[String]]
      val response : Future[Product] = passwordResp.map {
        case None => AuthFailure(AUTH_FAILURE_USER_NOT_FOUND)
        case Some(dbPassword) =>
          if (password == dbPassword) AuthSuccess
          else AuthFailure(AUTH_FAILURE_PASSWORD_INCORRECT)
      }
      response.pipeTo(sender())
  }
}