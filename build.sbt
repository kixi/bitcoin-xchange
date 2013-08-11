name := "bitcoin-xchange"

version := "0.1"

scalaVersion := "2.10.2"

resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= Dependencies.xchangeService

scalacOptions ++= Seq("-language:postfixOps","-deprecation","-feature")

javaOptions in run += "-javaagent:C:/program_dev/typesafe-console-developer-1.2.0/lib/weaver/aspectjweaver.jar"

javaOptions in run += "-Dorg.aspectj.tracing.factory=default"

javaOptions in run += "-Djava.library.path=../lib/sigar"
