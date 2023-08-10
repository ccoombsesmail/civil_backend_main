val sttpVersion = "3.3.13"
val circeVersion = "0.14.5"
val akkaVersion = "2.8.0"
val akkaHttpVersion = "10.5.0"
val akkaHttpCirceVersion = "1.39.2"
val zioVersion = "2.0.8"
val zioConfigVersion = "3.0.7"
val ZIOHttpVersion = "0.0.5"
val zioMetricsConnectorsVersion = "2.0.0-RC6" // metrics library for ZIO
val zioLoggingVersion = "2.0.0-RC10" // logging library for ZIO
val slf4jVersion = "1.7.36" // logging framework

ThisBuild / scalaVersion := "2.13.8"

flywayUrl := "jdbc:postgresql://localhost:5434/civil_main"
flywayUser := "postgres"
flywayPassword := "postgres"
val enumeratumVersion = "1.7.2"
resolvers += Resolver.mavenLocal

dockerExposedPorts ++= Seq(8090)
mainClass in (Compile, run) := Some("civil.Civil")

Global / onChangedBuildSource := ReloadOnSourceChanges
Compile / unmanagedSourceDirectories := (Compile / scalaSource).value :: Nil

ThisBuild / javaOptions ++= Seq(
  "--add-exports=java.base/jdk.internal.misc=ALL-UNNAMED"
)

ThisBuild / assemblyMergeStrategy := {
  case PathList("module-info.class")         => MergeStrategy.discard
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
    //    dependencyOverrides += "org.scala-lang" % "scala-collection-compat" % "2.13.6",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % zioVersion,
      "dev.zio" %% "zio-metrics-connectors-datadog" % "2.1.0", // DataDog client
      "dev.zio" %% "zio-metrics-connectors" % zioMetricsConnectorsVersion,
      //      "dev.zio" %% "zio-logging" % "2.1.13",
      "dev.zio" %% "zio-logging-slf4j" % zioLoggingVersion,
      "dev.zio" %% "zio-kafka" % "2.1.3",
      "dev.zio" %% "zio-json" % "0.4.2",
      "dev.zio" %% "zio-http" % ZIOHttpVersion,
      "org.postgresql" % "postgresql" % "42.5.4",
      "org.slf4j" % "slf4j-api" % slf4jVersion,
      "org.slf4j" % "slf4j-simple" % slf4jVersion
    )
  )
)

lazy val root = project
  .in(file("."))
  .enablePlugins(FlywayPlugin)
  .settings(
    name := "civil",
    assembly / mainClass := Some("civil.Civil"),
    libraryDependencies ++= Seq(
      "ch.megard" %% "akka-http-cors" % "1.2.0",
      "com.typesafe.akka" %% "akka-actor-typed" % "2.8.0",
      "com.typesafe" % "config" % "1.4.2",
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "com.lightbend.akka" %% "akka-stream-alpakka-s3" % "5.0.0",
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
      "io.circe" %% "circe-generic-extras" % "0.14.3",
      "io.getquill" %% "quill-jdbc-zio" % "4.6.0",
      "io.getquill" %% "quill-jdbc" % "4.6.0",
      "io.jsonwebtoken" % "jjwt-api" % "0.11.5",
      "io.jsonwebtoken" % "jjwt-impl" % "0.11.5",
      "io.jsonwebtoken" % "jjwt-jackson" % "0.11.5",
      "org.postgresql" % "postgresql" % "42.5.4",
      "com.github.jwt-scala" %% "jwt-circe" % "9.2.0",
      "ch.qos.logback" % "logback-classic" % "1.4.6" % Runtime,
      "com.beachape" %% "enumeratum-circe" % enumeratumVersion,
      "com.beachape" %% "enumeratum-quill" % enumeratumVersion,
      "io.scalaland" %% "chimney" % "0.7.1",
      "com.softwaremill.sttp.client3" %% "core" % "3.8.13",
      "com.softwaremill.sttp.client3" %% "zio" % "3.8.13",
      "com.softwaremill.sttp.client3" %% "circe" % "3.8.13",
      "edu.stanford.nlp" % "stanford-corenlp" % "4.5.2" artifacts (Artifact(
        "stanford-corenlp",
        "models"
      ), Artifact("stanford-corenlp")),
      "org.elastos.did" % "didsdk" % "2.2.4",

      //      "org.elastos.did" % "didsdk" % "2.2.4" exclude("io.jsonwebtoken", "jjwt-api") exclude("io.jsonwebtoken", "jjwt-impl") exclude("io.jsonwebtoken", "jjwt-jackson"),
      "com.googlecode.owasp-java-html-sanitizer" % "owasp-java-html-sanitizer" % "20220608.1"
    )
  )

enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)
