name := "bitcoin-xchange"

version := "0.1"

scalaVersion := "2.10.2"

resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= Dependencies.xchangeService

scalacOptions ++= Seq("-language:postfixOps","-deprecation","-feature")

