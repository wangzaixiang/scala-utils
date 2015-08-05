name := "utils-dbunit"

organization := "com.github.wangzaixiang"

version := "1.0.0-SNAPSHOT"

resolvers += "Sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

libraryDependencies ++= Seq(
  "com.github.wangzaixiang" %% "scala-sql" % "1.0.3-SNAPSHOT",
  "org.scala-lang.modules" %% "scala-xml" % "1.0.4" ,

  "com.github.wangzaixiang" %% "spray-json" % "1.3.3-SNAPSHOT",

  "com.h2database" % "h2" % "1.4.184" % "test"
)

publishMavenStyle := true

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (version.value.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

publishArtifact in Test := false

pomIncludeRepository := { _ => false }