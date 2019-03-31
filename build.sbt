enablePlugins(JavaAppPackaging, AshScriptPlugin)

name := "entity-store"

version := "0.1"

scalaVersion := "2.12.8"

dockerBaseImage := "openjdk:8-jre-alpine"
packageName in Docker := "entity-store"

val akkaVersion = "2.5.21"
val akkaHttpVersion = "10.1.7"
val circeVersion = "0.10.0"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
  "com.lightbend.akka" %% "akka-stream-alpakka-cassandra" % "1.0-RC1",
  "com.typesafe.akka" %% "akka-persistence-cassandra-launcher" % "0.93" % Test,

  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "de.heikoseeberger" %% "akka-http-circe" % "1.25.2",

  "org.scalatest" %% "scalatest" % "3.0.5" % Test
)
