name := "scala-utils"

version := "1.0"

scalaVersion in Global := "2.11.7"


lazy val dbunit = project
  .settings(
    resolvers += "Sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",

    libraryDependencies ++= Seq(
      "com.github.wangzaixiang" %% "scala-sql" % "1.0.3-SNAPSHOT",
      "org.scala-lang.modules" %% "scala-xml" % "1.0.4" ,

      "com.github.wangzaixiang" %% "spray-json" % "1.3.3-SNAPSHOT",

      "com.h2database" % "h2" % "1.4.184" % "test"
    )
  )
