name := "utils-dbunit"

organization := "com.github.wangzaixiang"

version := "1.0.1"

resolvers += "Sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

libraryDependencies ++= Seq(
  "com.github.wangzaixiang" %% "scala-sql" % "2.0.7",
  "org.scala-lang.modules" %% "scala-xml" % "2.0.1" ,
  "com.github.wangzaixiang" %% "spray-json" % "1.3.4",

  "com.h2database" % "h2" % "1.4.184" % "test",
  "junit" % "junit" % "4.12" % "test"
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

pomExtra := (
  <url>http://github.com/wangzaixiang/{name.value}</url>
    <licenses>
      <license>
        <name>BSD-style</name>
        <url>http://www.opensource.org/licenses/bsd-license.php</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <scm>
      <url>https://github.com/wangzaixiang/scala-utils.git</url>
      <connection>scm:git:git@github.com:wangzaixiang/scala-utils.git</connection>
    </scm>
    <developers>
      <developer>
        <id>wangzaixiang</id>
        <name>wangzaixiang</name>
        <url>http://wangzaixiang.github.io</url>
      </developer>
    </developers>)
