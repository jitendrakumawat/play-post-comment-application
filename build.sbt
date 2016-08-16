name := """msmp"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava, PlayEbean)

scalaVersion := "2.11.7"

resolvers +=
  "mvnrepo" at "http://mvnrepository.com/artifact/"

libraryDependencies ++= Seq(
  javaJdbc,
  cache,
  javaWs,
  "org.postgresql" % "postgresql" % "9.4.1208.jre7",
  "com.typesafe.play" %% "play-mailer" % "5.0.0-M1"
)


fork in run := true