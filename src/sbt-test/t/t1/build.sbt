import sjsonnew.support.scalajson.unsafe.Parser.parseFromFile

val common = Def.settings(
  libraryDependencies += "org.scalatest" %% "scalatest-wordspec" % "3.2.19" % Test,
)
val a1 = project.settings(common)
val a2 = project.settings(common)

TaskKey[Unit]("check") := {
  val x1 = parseFromFile(file("expect.json")).get
  val x2 = parseFromFile(file("failure-tests.json")).get
  assert(x1 == x2)
}
