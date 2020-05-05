name := "trading-journal"
 
version := "1.0" 
      
lazy val `trading-journal` = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.0"

libraryDependencies += guice
libraryDependencies += "com.typesafe.play" %% "play-slick" % "4.0.2"
libraryDependencies += "com.typesafe.play" %% "play-slick-evolutions" % "4.0.2"
libraryDependencies += "org.postgresql" % "postgresql" % "42.2.5"

libraryDependencies += "commons-io" % "commons-io" % "2.6"

libraryDependencies += "org.scalaj" %% "scalaj-http" % "2.4.2"

libraryDependencies += "com.sksamuel.scrimage" %% "scrimage-core" % "3.0.0-alpha4"
libraryDependencies += "com.sksamuel.scrimage" %% "scrimage-filters" % "3.0.0-alpha4"

scalacOptions += "-language:postfixOps"

// unmanagedResourceDirectories in Test <+=  baseDirectory ( _ /"target/web/public/test" )