name := "play-rconf"


version := "20.10"


homepage := Some(url("https://github.com/play-rconf/play-rconf"))


description := "Remote configuration for Play Framework"


startYear := Some(2018)


licenses += "MIT" -> url("https://raw.githubusercontent.com/play-rconf/play-rconf/master/LICENSE")


organization := "io.play-rconf"


organizationHomepage := Some(url("https://github.com/play-rconf"))


scalaVersion := "2.13.3"


resolvers += "jitpack" at "https://jitpack.io"


libraryDependencies ++= Seq(
  "com.github.play-rconf" % "play-rconf-sdk" % "release~18.05",
  "com.typesafe.play" %% "play-guice" % "2.8.3"
)


scalacOptions in ThisBuild ++= Seq(
  "-deprecation",
  "-unchecked"
)


javacOptions in ThisBuild ++= Seq(
  "-Xlint:cast",
  "-Xlint:deprecation",
  "-Xlint:divzero",
  "-Xlint:empty",
  "-Xlint:fallthrough",
  "-Xlint:finally",
  "-Xlint:unchecked"
)
