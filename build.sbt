name := "play-rconf"


organization := "io.play-rconf"


version := "18.04"


scalaVersion := "2.12.5"


scalacOptions in ThisBuild ++= Seq(
  "-deprecation",
  "-unchecked"
)


resolvers += "jitpack" at "https://jitpack.io"


libraryDependencies ++= Seq(
  "com.github.play-rconf" % "play-rconf-sdk" % "release~18.04u3",
  "com.typesafe.play" %% "play-guice" % "2.6.12"
)
