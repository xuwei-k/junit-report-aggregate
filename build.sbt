import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._

enablePlugins(SbtPlugin, ScriptedPlugin)
name := "junit-report-aggregate"
publishTo := (if (isSnapshot.value) None else localStaging.value)
Compile / unmanagedResources += (LocalRootProject / baseDirectory).value / "LICENSE.txt"
Compile / packageSrc / mappings ++= (Compile / managedSources).value.map { f =>
  (f, f.relativeTo((Compile / sourceManaged).value).get.getPath)
}
Compile / doc / scalacOptions ++= {
  val hash = sys.process.Process("git rev-parse HEAD").lineStream_!.head
  if (scalaBinaryVersion.value != "3") {
    Seq(
      "-sourcepath",
      (LocalRootProject / baseDirectory).value.getAbsolutePath,
      "-doc-source-url",
      s"https://github.com/xuwei-k/junit-report-aggregate/blob/${hash}€{FILE_PATH}.scala"
    )
  } else {
    Nil
  }
}
scalacOptions ++= {
  if (scalaBinaryVersion.value == "3") {
    Nil
  } else {
    Seq(
      "-Xsource:3",
    )
  }
}
scalacOptions ++= Seq(
  "-deprecation",
)
crossScalaVersions += "3.7.2"
pluginCrossBuild / sbtVersion := {
  scalaBinaryVersion.value match {
    case "2.12" =>
      (pluginCrossBuild / sbtVersion).value
    case _ =>
      "2.0.0-RC2"
  }
}
pomExtra := (
  <developers>
  <developer>
    <id>xuwei-k</id>
    <name>Kenji Yoshida</name>
    <url>https://github.com/xuwei-k</url>
  </developer>
</developers>
<scm>
  <url>git@github.com:xuwei-k/junit-report-aggregate.git</url>
  <connection>scm:git:git@github.com:xuwei-k/junit-report-aggregate.git</connection>
</scm>
)
organization := "com.github.xuwei-k"
homepage := Some(url("https://github.com/xuwei-k/junit-report-aggregate"))
licenses := List(
  "MIT License" -> url("https://opensource.org/licenses/mit-license")
)
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  releaseStepCommandAndRemaining("+ publishSigned"),
  releaseStepCommandAndRemaining("sonaRelease"),
  setNextVersion,
  commitNextVersion,
  pushChanges
)
description := "sbt plugin for aggregate junit reports"
scriptedLaunchOpts += "-Dplugin.version=" + version.value
scriptedBufferLog := false
sbtPluginPublishLegacyMavenStyle := {
  sys.env.isDefinedAt("GITHUB_ACTION") || isSnapshot.value
}
