name := "akka-essentials"

version := "0.1"

scalaVersion := "2.13.3"

val AkkaVersion = "2.6.8"
val ScalaTest = "3.2.0"
libraryDependencies += "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % AkkaVersion % Test
libraryDependencies += "org.scalactic" %% "scalactic" % ScalaTest
libraryDependencies += "org.scalatest" %% "scalatest" % ScalaTest % "test"