package actors

import actors.ActorsChildExercise.WordCounterMaster.{Initialize, WordCountReply, WordCountTask}
import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ActorsChildExercise extends App {


  object WordCounterMaster {
    case class Initialize(nChildren: Int)
    case class WordCountTask(text: String)
    case class WordCountReply(nWords: Int)
  }

  class WordCounterMaster extends Actor {
    var workers = Map.empty[Int, ActorRef]

    override def receive: Receive = {
      case Initialize(n) =>
        workers = (1 to n).map(counter => counter -> context.actorOf(Props[WordCounterWorker], s"worker$counter")).toMap
        context.become(task(1))
    }

    def task(counter: Int): Receive = {
      case WordCountTask(text: String) =>
        workers.get(counter).map(worker => {
          println(s"Sending $text to ${worker.path.name}")
          worker ! text
          context.become(task(if ((counter + 1) == 11) 1 else counter+1))
        })
      case WordCountReply(words) =>
        println(s"Got words count $words from [${sender.path.name}]")
    }
  }

  class WordCounterWorker extends Actor {
    override def receive: Receive = {
      case text: String =>
        println(s"[${self.path.name}] Received Text $text")
        val words = text.split(" ").length
        sender() ! WordCountReply(words)
    }
  }

  private val system: ActorSystem = ActorSystem("master-worker")
  private val master: ActorRef = system.actorOf(Props[WordCounterMaster], "master")
  master ! Initialize(10)
  (1 to 15) foreach (count =>   master ! WordCountTask(s"I am Learning Scala$count!"))
  system.terminate()
}
