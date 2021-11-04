//import play.twirl.sbt.SbtTwirl
// resolvers += "Sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

lazy val codeGenerator = project.in(file(".")).enablePlugins(SbtTwirl)

libraryDependencies ++= Seq(
  "mysql" % "mysql-connector-java" % "5.1.34",
  "com.github.wangzaixiang" %% "scala-sql" % "2.0.7"
)