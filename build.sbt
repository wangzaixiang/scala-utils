name := "scala-utils"

version := "1.0"

scalaVersion in Global := "2.11.7"

lazy val dbunit = project

lazy val codeGenerator = project.in(file("code-generator"))

//lazy val codeGenerator = project.in(file("code-generator")).enablePlugins(SbtTwirl)
