lazy val b2c = project.in(file(".")).enablePlugins(SbtTwirl)

libraryDependencies += "org.scalameta" %% "scalafmt-dynamic" % "3.0.8"

javacOptions := Seq("-parameters")