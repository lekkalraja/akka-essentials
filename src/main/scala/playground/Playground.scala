package playground

import akka.actor.ActorSystem

object Playground extends App {

  private val actor: ActorSystem = ActorSystem("HelloAkka")

  println(actor.name)
}