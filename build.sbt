val scala3Version = "3.7.3"

lazy val root = project
  .in(file("."))
  .settings(
    name := "llm-pipeline",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    libraryDependencies ++= Seq(
      "com.anthropic" % "anthropic-java" % "2.8.1",
      "org.apache.pdfbox" % "pdfbox" % "3.0.5",
      "com.github.pureconfig" %% "pureconfig-core" % "0.17.9",
      "org.slf4j" % "slf4j-simple" % "2.0.17",
      "org.typelevel" %% "cats-effect" % "3.6.3",
      "org.scalameta" %% "munit" % "1.0.0" % Test
    ),
    Compile / run / fork := true
  )
