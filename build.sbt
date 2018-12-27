
name := "Simple Progress Bar"

resolvers += "jitpack" at "https://jitpack.io"

lazy val root =
  (project in file("."))
    .settings(
      version := "1.4.0",
      scalaVersion := "2.12.3",
      crossScalaVersions := Seq("2.11.8"),
      scalacOptions ++= Seq(
        "-encoding",
        "UTF-8",
        "-deprecation",
        "-unchecked",
        "-feature",
        "-Ywarn-adapted-args",
        "-Ywarn-inaccessible",
        "-Ywarn-unused",
        "-Ywarn-dead-code",
        "-Ywarn-unused-import",
        "-Ywarn-value-discard",
        "-Xfatal-warnings"),
      scapegoatVersion := "1.3.0",
      scapegoatReports := Seq("html"),
      coverageMinimum := 80,
      coverageHighlighting := true,
      coverageOutputXML := false,
      libraryDependencies ++= Seq(
        "com.github.morgen-peschke" %% "scala-commons" % "v1.0.1",
        "org.scalactic" %% "scalactic" % "3.0.1" % "test",
        "org.scalatest" %% "scalatest" % "3.0.2" % "test"))

lazy val example =
  (project in file("example"))
    .dependsOn(root)
    .settings(
      version := "0.0.0",
      scalaVersion := "2.12.3",
      crossScalaVersions := Seq("2.11.8"),
      publish := {},
      publishLocal := {},
      publishArtifact := false,
      scalacOptions ++= Seq(
        "-encoding",
        "UTF-8",
        "-deprecation",
        "-unchecked",
        "-feature",
        "-Ywarn-adapted-args",
        "-Ywarn-inaccessible",
        "-Ywarn-unused",
        "-Ywarn-dead-code",
        "-Ywarn-unused-import",
        "-Ywarn-value-discard",
        "-Xfatal-warnings"),
        libraryDependencies += "com.github.scopt" %% "scopt" % "3.7.1")
