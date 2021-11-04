name := "scala-utils"

version := "1.0"

scalaVersion in Global := "2.12.15"

lazy val dbunit = project

lazy val codeGenerator = project.in(file("code-generator"))

lazy val b2c = project

//lazy val codeGenerator = project.in(file("code-generator")).enablePlugins(SbtTwirl)
