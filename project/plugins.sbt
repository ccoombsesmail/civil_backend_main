resolvers += "Flyway" at "https://davidmweber.github.io/flyway-sbt.repo"
addSbtPlugin("io.github.davidmweber" % "flyway-sbt" % "7.4.0")


addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.8.1")
addSbtPlugin("org.scoverage"    % "sbt-scoverage"       % "1.8.2")
addSbtPlugin("org.scalameta"    % "sbt-scalafmt"        % "2.4.2")
addSbtPlugin("ch.epfl.scala"    % "sbt-scalafix"        % "0.9.29")
addSbtPlugin("com.eed3si9n"     % "sbt-assembly"        % "1.0.0")
addSbtPlugin("com.timushev.sbt" % "sbt-updates"         % "0.5.3")
addSbtPlugin("com.eed3si9n"     % "sbt-buildinfo"       % "0.9.0")

addSbtPlugin("io.github.scala-ts" % "sbt-scala-ts" % "0.5.15")
