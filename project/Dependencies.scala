import sbt._

object Dependencies {
  // versions
  val akkaVersion = "2.2.0"

  // libraries
  val scalaTest = "org.scalatest" %% "scalatest" % "1.9.1"
  val slf4s = "com.weiglewilczek.slf4s" % "slf4s_2.9.1" % "1.0.7"
  val slf4jlog4j = "org.slf4j" % "slf4j-log4j12" % "1.7.5"

  val akkaActor = "com.typesafe.akka" %% "akka-actor" % akkaVersion
  val akkaRemote = "com.typesafe.akka" %% "akka-remote" % akkaVersion
  val akkaTestkit = "com.typesafe.akka" %% "akka-testkit" % akkaVersion
  val akkaAtmos = "com.typesafe.atmos" % "trace-akka-2.2.0_2.10" % "1.2.0"
  val logback = "ch.qos.logback" % "logback-classic" % "1.0.7"
  val redisclient = "net.debasishg" % "redisclient_2.10" % "2.10"
  val jodaTime = "joda-time" % "joda-time" % "2.1"
  val jodaConvert = "org.joda" % "joda-convert" % "1.2"

  val xchangeService = Seq(
    scalaTest % "test",
    slf4s,
    slf4jlog4j,
    akkaActor,
    akkaRemote,
    akkaTestkit % "test",
    akkaAtmos,
    jodaTime,
    jodaConvert,
    logback
  )
}
