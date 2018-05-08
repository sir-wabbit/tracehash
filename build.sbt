lazy val common = Seq(
  organization      := "com.alexknvl",
  version           := "0.0.1-SNAPSHOT",
  scalaVersion      := "2.12.6",
  libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % "test",
  libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.14.0" % "test")


lazy val root = project.in(file("."))
  .settings(common : _*)
