import sbt.Keys._

lazy val root = project.in(file("."))
  .enablePlugins(ScalaJSPlugin).settings(
  credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),
  resolvers += "NextWave Repo" at "http://maxdevmaster.cloudapp.net:4343/artifactory/nxtwv-maven/",
  publishTo := Some("NextWave Repo" at "http://maxdevmaster.cloudapp.net:4343/artifactory/nxtwv-maven/")
)

lazy val demo = (project in file("demo"))
  .settings(demoSettings:_*)
  .enablePlugins(ScalaJSPlugin)
  .dependsOn(root)


lazy val demoSettings = Seq(
  name := s"webrtc-demo",
  scalaVersion := "2.11.6",
  libraryDependencies ++= Seq(
    "org.scala-js" %%% "scalajs-dom" % "0.9.0"
  )
)

name := "ScalaJS WebRTC library. Based on SimpleWebRTC."

normalizedName := "scala-js-webrtc"

version := "0.0.1-SNAPSHOT"

organization := "io.surfkit"

scalaVersion := "2.11.8"

crossScalaVersions := Seq("2.10.4", "2.11.7")

libraryDependencies ++= Seq(
  "org.scala-js" %%% "scalajs-dom" % "0.9.0"
)

jsDependencies in Test += RuntimeDOM

homepage := Some(url("http://www.surfkit.io/"))

licenses += ("MIT License", url("http://www.opensource.org/licenses/mit-license.php"))

scmInfo := Some(ScmInfo(
    url("https://github.com/coreyauger/scala-js-webrtc"),
    "scm:git:git@github.com/coreyauger/scala-js-webrtc.git",
    Some("scm:git:git@github.com:coreyauger/scala-js-webrtc.git")))


pomExtra := (
  <developers>
    <developer>
      <id>coreyauger</id>
      <name>Corey Auger</name>
      <url>https://github.com/coreyauger/</url>
    </developer>
  </developers>
)

pomIncludeRepository := { _ => false }
