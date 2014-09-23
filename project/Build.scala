import sbt._
import Keys._
import play.Play.autoImport._
import Resolvers._

object ApplicationBuild extends Build {


  lazy val main = Project("root", file("."))
    .enablePlugins(play.PlayScala)
    .settings(
      scalaVersion := "2.11.2",
      resolvers ++= Seq(
        "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
        "Maven Central Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
      ),
      libraryDependencies ++= Seq(
        ws,
       "jp.co.bizreach"         %% "play2-handlebars"          % "0.2-SNAPSHOT",
       "com.propensive"         %% "rapture-io"                % "0.9.1",
       "org.reactivemongo"      %% "play2-reactivemongo"       % "0.10.5.akka23-SNAPSHOT",
       "org.jsoup"               % "jsoup"                     % "1.7.3",
       "com.google.oauth-client" % "google-oauth-client"       % "1.19.0", 
	     "de.flapdoodle.embed"     % "de.flapdoodle.embed.mongo" % "1.46.1"
	    )
    )
}
