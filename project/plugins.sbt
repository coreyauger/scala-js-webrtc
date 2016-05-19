resolvers in ThisBuild += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers in ThisBuild += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.3.4")

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.8")

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.0.3")
