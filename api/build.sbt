name := "trading-journal"
 
version := "1.0" 
      
lazy val `trading-journal` = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.2"

libraryDependencies += guice
libraryDependencies += "com.typesafe.play" %% "play-slick" % "4.0.2"
libraryDependencies += "com.typesafe.play" %% "play-slick-evolutions" % "4.0.2"
libraryDependencies += "org.postgresql" % "postgresql" % "42.2.5"

libraryDependencies += "commons-io" % "commons-io" % "2.6"

libraryDependencies += "org.scalaj" %% "scalaj-http" % "2.4.2"

scalacOptions += "-language:postfixOps"

libraryDependencies += "com.github.andyglow" %% "websocket-scala-client" % "0.3.0"

// unmanagedResourceDirectories in Test <+=  baseDirectory ( _ /"target/web/public/test" )