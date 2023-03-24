val tapirVersion = "0.19.0-M5"
val sttpVersion = "3.3.13"
val circeVersion = "0.14.1"
val akkaVersion = "2.6.15"
val akkaHttpVersion = "10.2.6"
val akkaHttpCirceVersion = "1.31.0"
val zioVersion = "1.0.14"
val zioConfigVersion = "1.0.6"


flywayUrl := "jdbc:postgresql://localhost:5433/civil_main"
flywayUser := "postgres"
flywayPassword := "postgres"
val enumeratumVersion = "1.7.0"
resolvers += Resolver.mavenLocal

dockerExposedPorts ++= Seq(8090)
mainClass in (Compile, run) := Some("civil.Civil")

Global / onChangedBuildSource := ReloadOnSourceChanges
ThisBuild / scalaVersion := "2.13.5"
// ThisBuild / sbtPlugin := true
Compile / unmanagedSourceDirectories := (Compile / scalaSource).value :: Nil

//ThisBuild / assemblyMergeStrategy in assembly := {
//  case PathList("META-INF", xs @ _*) => MergeStrategy.defaultMergeStrategy
//  case PathList("reference.conf")    => MergeStrategy.concat
//  case x                             => MergeStrategy.last
//
//}
ThisBuild / assemblyMergeStrategy := {
  case PathList("module-info.class") => MergeStrategy.discard
  case x if x.endsWith("/module-info.class") => MergeStrategy.discard
  case x =>
    val oldStrategy = (ThisBuild / assemblyMergeStrategy).value
    oldStrategy(x)
}

lazy val sysProperty = taskKey[Unit](s"Set prop")
sysProperty := {
  System.setProperty("aws.region", "us-west-1");
}
inThisBuild(
  List(
    version := "0.2.0",
    organization := "ccoombsesmail",
    dependencyOverrides += "org.scala-lang" % "scala-collection-compat" % "2.13.6",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % zioVersion,
      "dev.zio" %% "zio-config" % zioConfigVersion,
      "dev.zio" %% "zio-config-magnolia" % zioConfigVersion,
      "dev.zio" %% "zio-config-typesafe" % zioConfigVersion,
      "dev.zio" %% "zio-streams" % zioVersion,
      "dev.zio" %% "zio-kafka" % "0.17.5",
      "dev.zio" %% "zio-json" % "0.1.5",
      "org.postgresql" % "postgresql" % "42.2.23"
    )
  )
)

// lazy val root = project.in(file("."))

// lazy val config = project
//scalatsUnionWithLiteral

lazy val root = project
  .in(file("."))
  .enablePlugins(FlywayPlugin)
  .settings(
    name := "civil",
    assembly / mainClass := Some("civil.Civil"),
    libraryDependencies ++= Seq(
      "ch.megard" %% "akka-http-cors" % "1.1.2",
      "com.softwaremill.sttp.tapir" %% "tapir-core" % "0.19.0-M5",
      "com.softwaremill.sttp.tapir" %% "tapir-akka-http-server" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-zio" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-zio-http" % tapirVersion,
      "com.typesafe.akka" %% "akka-actor-typed" % "2.6.15",
      "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % "0.19.0-M5",
      "com.typesafe" % "config" % "1.4.2",
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "com.lightbend.akka" %% "akka-stream-alpakka-s3" % "3.0.3",
      "com.typesafe.akka" %% "akka-http-xml" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "de.heikoseeberger" %% "akka-http-circe" % akkaHttpCirceVersion,
      "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
      "org.json4s" %% "json4s-jackson" % "3.7.0-RC1",
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,
      "io.circe" %% "circe-refined" % circeVersion,
      "io.circe" %% "circe-generic-extras" % circeVersion,
      "io.getquill" %% "quill-jdbc-zio" % "3.9.0",
      "io.getquill" %% "quill-jdbc" % "3.4.10",
      "io.jsonwebtoken" %  "jjwt-api" % "0.11.1",
      "io.jsonwebtoken" %  "jjwt-impl" % "0.11.1",
      "io.jsonwebtoken" %  "jjwt-jackson" % "0.11.1",
      "org.postgresql" % "postgresql" % "42.3.6",
      "com.github.jwt-scala" %% "jwt-circe" % "9.0.5",
      "ch.qos.logback" % "logback-classic" % "1.2.11" % Runtime,
      "com.beachape" %% "enumeratum-circe" % enumeratumVersion,
      "com.beachape" %% "enumeratum-quill" % enumeratumVersion,
      "io.scalaland" %% "chimney" % "0.6.1",
      "com.softwaremill.sttp.client3" %% "core" % "3.8.3",
      "com.softwaremill.sttp.client3" %% "zio1" % "3.8.3",
      "com.softwaremill.sttp.client3" %% "circe" % "3.8.3",
      "edu.stanford.nlp" % "stanford-corenlp" % "4.4.0" artifacts (Artifact(
        "stanford-corenlp",
        "models"
      ), Artifact("stanford-corenlp")),
      "org.elastos.did" % "didsdk" % "2.2.4",

      //      "org.elastos.did" % "didsdk" % "2.2.4" exclude("io.jsonwebtoken", "jjwt-api") exclude("io.jsonwebtoken", "jjwt-impl") exclude("io.jsonwebtoken", "jjwt-jackson"),
      "com.googlecode.owasp-java-html-sanitizer" % "owasp-java-html-sanitizer" % "20211018.2"
    )
  )

enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)
