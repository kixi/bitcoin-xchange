name := "bitcoin-exchange"

version := "0.1"

scalaVersion := "2.9.1"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

atmosSettings

javaOptions in run ++= Seq(
        "-javaagent:C:/program_dev/typesafe-console-developer-1.2.0/lib/weaver/aspectjweaver.jar",
        "-Dorg.aspectj.tracing.factory=default",
        "-Djava.library.path=C:/program_dev/typesafe-console-developer-1.2.0/lib/sigar"
      )

fork in run := true

libraryDependencies += "org.scalatest" %% "scalatest" % "1.6.1" % "test"

libraryDependencies += "com.weiglewilczek.slf4s" %% "slf4s" % "1.0.7"

libraryDependencies += "org.slf4j" % "slf4j-log4j12" % "1.7.5"

libraryDependencies += "com.typesafe.akka" % "akka-actor" % "2.0.5"

libraryDependencies += "com.typesafe.akka" % "akka-remote" % "2.0.5"

libraryDependencies += "com.typesafe.akka" % "akka-testkit" % "2.0.5" % "test"

libraryDependencies += "net.debasishg" % "redisclient_2.9.1" % "2.9"

libraryDependencies += "com.typesafe.atmos" % "trace-akka-2.0.5" % "1.2.0"
