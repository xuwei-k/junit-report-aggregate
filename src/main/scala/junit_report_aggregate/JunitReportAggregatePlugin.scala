package junit_report_aggregate

import sbt.*
import sbt.Keys.*
import sjsonnew.Builder
import sjsonnew.JsonFormat
import sjsonnew.JsonWriter
import sjsonnew.BasicJsonProtocol.*
import java.nio.file.Files
import sjsonnew.support.scalajson.unsafe.PrettyPrinter
import scala.xml.XML
import java.io.IOException
import scala.util.control.NonFatal

object JunitReportAggregatePlugin extends AutoPlugin {
  object autoImport {
    val aggregateJunitReports = taskKey[Seq[FailureTest]]("aggregate junit reports")
    val aggregateJunitReportsPrint = taskKey[String]("print failure test result")
    val aggregateJunitReportsWrite = taskKey[File]("write json file")
    val aggregateJunitReportsOutput = settingKey[File](s"output dir for ${aggregateJunitReportsWrite.key.label}")
  }

  override def trigger = allRequirements

  import autoImport.*

  // https://github.com/sbt/sbt/blob/v1.7.0/main/src/main/scala/sbt/plugins/JUnitXmlReportPlugin.scala
  private[this] val testReportsDirectory = SettingKey[File]("testReportsDirectory")

  override def buildSettings: Seq[Def.Setting[?]] = Seq(
    aggregateJunitReportsOutput := (LocalRootProject / baseDirectory).value / "failure-tests.json",
    aggregateJunitReports / testReportsDirectory := {
      (LocalRootProject / target).value / "aggregate-junit-reports"
    },
    aggregateJunitReportsPrint := {
      val j = aggregateJunitReports.value.toPrettyJsonString
      println(j)
      j
    },
    aggregateJunitReportsWrite := {
      val j = aggregateJunitReports.value.toPrettyJsonString
      val f = aggregateJunitReportsOutput.value
      IO.write(f, j)
      f
    },
    aggregateJunitReports := {
      val s = state.value
      val aggregateDir = (aggregateJunitReports / testReportsDirectory).value
      IO.delete(aggregateDir)
      IO.createDirectory(aggregateDir)
      val log = streams.value.log
      val extracted = Project.extract(s)
      extracted.structure.allProjectRefs.flatMap { p =>
        val dir =
          extracted.getOpt(p / Test / testReportsDirectory).getOrElse(extracted.get(p / target) / "test-reports")
        val projectId = p.project
        (dir ** "*.xml").get().flatMap { report =>
          val reportPath = report.getCanonicalFile.toPath
          try {
            Files.copy(reportPath, (aggregateDir / report.getName).getCanonicalFile.toPath)
          } catch {
            case e: IOException =>
              val fileName = s"${projectId}-${report.getName}"
              log.warn(s"retry copy with different name $fileName $e")
              Files.copy(reportPath, (aggregateDir / fileName).getCanonicalFile.toPath)
          }
          try {
            val xml = XML.loadFile(report)
            (xml \ "testcase").withFilter(x => (x \ "failure").nonEmpty || (x \ "error").nonEmpty).map { t =>
              val className = (t \ "@classname").text
              FailureTest(
                project = projectId,
                className = className,
                name = (t \ "@name").text,
                message = (t \ "failure" \ "@message").text ++ (t \ "error" \ "@message").text,
                rerun = s"${projectId}/testOnly ${className}",
              )
            }
          } catch {
            case NonFatal(e) =>
              log.error(s"error during parse junit report xml $e")
              Nil
          }
        }
      }.sorted
    }
  )

  private[this] implicit class JsonOps[A](private val self: A) extends AnyVal {
    def toPrettyJsonString(implicit writer: JsonWriter[A]): String = {
      val builder = new Builder(sjsonnew.support.scalajson.unsafe.Converter.facade)
      writer.write(self, builder)
      PrettyPrinter.apply(builder.result.getOrElse(sys.error("invalid json")))
    }
  }

  final case class FailureTest(project: String, className: String, name: String, message: String, rerun: String)

  object FailureTest {
    implicit val orderInstance: Ordering[FailureTest] = Ordering.by { a =>
      (a.project, a.className, a.name, a.message, a.rerun)
    }
    implicit val jsonFormatInstance: JsonFormat[FailureTest] = {
      import sjsonnew.BasicJsonProtocol.*
      caseClass5(FailureTest.apply, FailureTest.unapply)(
        "project",
        "className",
        "name",
        "message",
        "rerun",
      )
    }
  }
}
