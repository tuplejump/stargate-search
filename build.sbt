name := """play-scala"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  "com.datastax.cassandra" % "cassandra-driver-core" % "2.0.3",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.4.1.1",
  "com.fasterxml.jackson.module" % "jackson-module-scala_2.10" % "2.4.1",
  cache,
  ws
)
