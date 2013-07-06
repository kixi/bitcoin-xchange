name := "bitcoin-exchange"

version := "0.1"

scalaVersion := "2.9.1"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies += "org.scalatest" %% "scalatest" % "1.6.1" % "test"

libraryDependencies += "com.weiglewilczek.slf4s" %% "slf4s" % "1.0.7"

libraryDependencies += "org.slf4j" % "slf4j-log4j12" % "1.7.5"

libraryDependencies += "com.typesafe.akka" % "akka-actor" % "2.0.5"

libraryDependencies += "com.typesafe.akka" % "akka-testkit" % "2.0.5" % "test"
