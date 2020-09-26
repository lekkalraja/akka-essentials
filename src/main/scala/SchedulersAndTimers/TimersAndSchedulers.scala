package SchedulersAndTimers

import SchedulersAndTimers.TimersAndSchedulers.TimersBasedSelfClosingActor.TimersBasedSelfClosingActorKey
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Cancellable, PoisonPill, Props, Timers}

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

object TimersAndSchedulers extends App {

  class SimpleActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(s"[Simple-Actor] Got Message : ${message.toString}")
    }
  }

  private val system: ActorSystem = ActorSystem("TimersAndSchedulers")

  system.log.info("Logging from System")
  private val simpleActor: ActorRef = system.actorOf(Props[SimpleActor], "simpleActor")
  simpleActor ! "Reminder"

  import system.dispatcher
  system.scheduler.scheduleOnce(2 second, simpleActor, "Reminder") //(system.dispatcher) // Schedule Only Once

  private val cancellable: Cancellable = system.scheduler.scheduleWithFixedDelay(1 second, 2 seconds, simpleActor, "HeartBeat") //(system.dispatcher)
  //Thread.sleep(4000)
  system.scheduler.scheduleOnce(4 seconds){
    system.log.info("[Simple-Actor] Triggered Cancelling Scheduler after 4 seconds")
    cancellable.cancel()
  }

  //cancellable.cancel()// can cancel scheduler at any time
  Thread.sleep(10000)

  /**
   * Exercise : Implement a self-closing actor
   *  - if the actor receives a message (anything), you have 1 second to send it another message
   *  - if the time window expires, the actor will stop itself
   *  - if you send another message, the time window is reset
   */

  class SelfClosingActor extends Actor with ActorLogging {

    def closer : Cancellable = context.system.scheduler.scheduleOnce(1 second) {
      log.info("[Self-Closing Actor] Timeout!!! Closing Actor.")
      self ! PoisonPill
    }

    override def receive: Receive = selfClosing(closer)

    def selfClosing(cancel: Cancellable) : Receive = {
      case message =>
        cancel.cancel()
        log.info(s"[Self-Closing Actor] Received message : ${message.toString}")
        context.become(selfClosing(closer))
    }
  }

  private val selfClosingActor: ActorRef = system.actorOf(Props[SelfClosingActor], "selfClosingActor")

  private val cancellable1: Cancellable = system.scheduler.scheduleWithFixedDelay(10 millis, 500 millis, selfClosingActor, "HeartBeat")
  system.scheduler.scheduleOnce(5 seconds) {
    cancellable1.cancel()
  }
  system.scheduler.scheduleWithFixedDelay(7 seconds, 1 second, selfClosingActor, "HeartBeat-ForFail")

  Thread.sleep(10000)

  /**
   * Timers : Schedule message to self, from within
   */

  case object TimerKey
  case object Start
  case object Stop

  class TimerBasedActor extends Actor with ActorLogging with Timers {

    /**
     * Each timer has a key and if a new timer with same key is started
     * the previous is cancelled. It is guaranteed that a message from the
     * previous timer is not received, even if it was already enqueued
     * in the mailbox when the new timer was started.
     */
    timers.startSingleTimer(TimerKey, Start, 1 second)

    override def receive: Receive = {
      case Start =>
        log.info("[TimerBasedActor] Started !!!")
        timers.startTimerAtFixedRate(TimerKey, "HeartBeat", 1 second)
      case message : String => log.info(s"[TimerBasedActor] Received message $message")
      case Stop =>
        log.info("[TimerBasedActor] Received Stop signal to stop Timers")
        timers.cancel(TimerKey) // you can use timers.cancelAll() to stop all timers
    }
  }

  private val timerBasedActor: ActorRef = system.actorOf(Props[TimerBasedActor], "timerBasedActor")
  system.scheduler.scheduleOnce(4 seconds, timerBasedActor, Stop)

  Thread.sleep(10000)
  /**
   * Timers based self closing Actor
   */

  object TimersBasedSelfClosingActor {
    case object TimersBasedSelfClosingActorKey
    case object Stop
  }

  class TimersBasedSelfClosingActor extends Actor with ActorLogging with Timers {
    override def receive: Receive = {
      case message: String =>
        log.info(s"[Timer-SelfClosing-Actor] Received : $message")
        timers.startTimerAtFixedRate(TimersBasedSelfClosingActorKey, Stop, 1 second)
      case Stop =>
        log.info(s"[Timer-SelfClosing-Actor] Received Stop Signal")
        context.stop(self)
    }
  }

  private val timersBasedSelfClosingActor: ActorRef = system.actorOf(Props[TimersBasedSelfClosingActor], "timersBasedSelfClosingActor")
  private val cancellable2: Cancellable = system.scheduler.scheduleWithFixedDelay(500 millis, 500 millis, timersBasedSelfClosingActor, "HeartBeat")
  Thread.sleep(4000)
  cancellable2.cancel()
  Thread.sleep(2000)
  timersBasedSelfClosingActor ! "Ping"


  system.scheduler.scheduleOnce(20 seconds) {
    system.log.info("Triggered System termination  after 20 seconds")
    system.terminate()
  }
}