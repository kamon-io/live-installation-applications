scalaVersion := "2.13.8"
lazy val AkkaVersion = "2.6.19"
lazy val AkkaHttpVersion = "10.2.9"


lazy val api = (project in file("api"))
  .settings(Seq(
    libraryDependencies ++= Seq(
      "com.typesafe.akka"   %% "akka-http"              % AkkaHttpVersion,
      "com.typesafe.akka"   %% "akka-http-spray-json"   % AkkaHttpVersion,
      "com.typesafe.akka"   %% "akka-actor"             % AkkaVersion,
      "com.typesafe.akka"   %% "akka-stream"            % AkkaVersion,
      "com.typesafe.akka"   %% "akka-slf4j"             % AkkaVersion,
      "ch.qos.logback"      %  "logback-classic"        % "1.2.11",

      "io.kamon" %% "kamon-bundle" % "2.5.8",
      "io.kamon" %% "kamon-apm-reporter" % "2.5.8",
    )
  ))


lazy val concierge = (project in file("concierge"))
  .settings(Seq(
    libraryDependencies ++= Seq(
      "com.typesafe.akka"   %% "akka-http"              % AkkaHttpVersion,
      "com.typesafe.akka"   %% "akka-http-spray-json"   % AkkaHttpVersion,
      "com.typesafe.akka"   %% "akka-actor"             % AkkaVersion,
      "com.typesafe.akka"   %% "akka-remote"            % AkkaVersion,
      "com.typesafe.akka"   %% "akka-cluster"           % AkkaVersion,
      "com.typesafe.akka"   %% "akka-cluster-sharding"  % AkkaVersion,
      "com.typesafe.akka"   %% "akka-slf4j"             % AkkaVersion,
      "ch.qos.logback"      %  "logback-classic"        % "1.2.11",
      "com.orbitz.consul"   %  "consul-client"          % "1.5.3",

      "io.kamon" %% "kamon-bundle" % "2.5.8",
      "io.kamon" %% "kamon-apm-reporter" % "2.5.8",
    )
  ))


lazy val bouncer = (project in file("bouncer"))
  .enablePlugins(PlayScala, JavaAgent)
  .settings(Seq(
    libraryDependencies ++= Seq(
      guice,
      "com.typesafe.play" %% "play-slick" % "5.0.2",
      "org.postgresql" % "postgresql" % "42.5.0",

      "io.kamon" %% "kamon-bundle" % "2.5.8",
      "io.kamon" %% "kamon-apm-reporter" % "2.5.8",
    )
  ))
