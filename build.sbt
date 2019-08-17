name := "trading-journal"
 
version := "1.0" 
      
lazy val `trading-journal` = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.0"

libraryDependencies += guice
libraryDependencies += "com.typesafe.play" %% "play-slick" % "4.0.2"
libraryDependencies += "com.typesafe.play" %% "play-slick-evolutions" % "4.0.2"
libraryDependencies += "org.postgresql" % "postgresql" % "42.2.5"

// unmanagedResourceDirectories in Test <+=  baseDirectory ( _ /"target/web/public/test" )