package patterns

import akka.actor.Actor
import patterns.DBActor.{Read, Write}

// Assume it is a (redis, aerospike) i.e. Key-Value Database
object DBActor {
  case class Read(key: String)
  case class Write(key: String, value: String)
}
class DBActor extends Actor {
  override def receive: Receive = kvDB(Map())
  def kvDB(store: Map[String, String]) : Receive = {
    case Read(key) => sender() ! store.get(key)
    case Write(key, value) => context.become(kvDB(store + (key -> value)))
  }
}