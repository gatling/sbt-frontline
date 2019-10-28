package io.gatling.frontline.sbt

import java.io.File

import scala.xml.parsing.ConstructingParser

import org.zeroturnaround.zip.ZipUtil
import org.zeroturnaround.zip.commons.FileUtilsV2_2
import sbt.io.{ IO, Using }

object FrontLineFatJar {

  case class ModuleId(
      organization: String,
      name: String,
      revision: String
  )

  case class Module(
      id: ModuleId,
      jar: File,
      callers: Seq[ModuleId]
  )

  def fatjar(
      ivyReport: File,
      organisation: String,
      target: File,
      fullClasspath: Seq[File],
      fatJarName: String
  ): File = {
    val doc = ConstructingParser
      .fromSource(scala.io.Source.fromFile(ivyReport), preserveWS = false)
      .document()

    val info = (doc \ "info").head

    val rootModule =
      Module(
        ModuleId(
          info.attribute("organisation").get.text,
          info.attribute("module").get.text,
          info.attribute("revision").get.text
        ),
        new File("FAKE"),
        Nil
      )

    val modules: Seq[Module] =
      (for {
        module <- doc \ "dependencies" \ "module"
        moduleRevision <- module \ "revision"
        if moduleRevision.attribute("evicted").isEmpty

        moduleId = ModuleId(
          module.attribute("organisation").get.text,
          module.attribute("name").get.text,
          moduleRevision.attribute("name").get.text
        )

        callers: Seq[ModuleId] = (moduleRevision \ "caller").map { caller =>
          ModuleId(
            caller.attribute("organisation").get.text,
            caller.attribute("name").get.text,
            caller.attribute("callerrev").get.text
          )
        }

        artifacts <- moduleRevision \ "artifacts" if artifacts.nonEmpty

        jars = for {
          artifact <- artifacts \ "artifact"
          if artifact.attribute("ext").get.text == "jar"
        } yield new File(artifact.attribute("location").get.text)

      } yield jars.headOption.map(jar => Module(moduleId, jar, callers))).flatten

    def removeModulesByAncestorWithOrg(mods: Seq[Module], orgs: Set[String]): Seq[Module] = {

      val allModsById = mods.map(mod => mod.id -> mod).toMap

      def hasAncestorWithOrg(mod: Module): Boolean =
        if (orgs.contains(mod.id.organization)) {
          true
        } else {
          mod.callers.flatMap(allModsById.get).exists(hasAncestorWithOrg)
        }

      mods.filterNot(hasAncestorWithOrg)
    }

    val nonGatlingModules =
      removeModulesByAncestorWithOrg(modules :+ rootModule, Set("io.gatling", "io.gatling.highcharts", "io.gatling.frontline"))
        .filterNot(_ == rootModule)

    val classDirectories = fullClasspath.filter(_.isDirectory)

    IO.withTemporaryDirectory(
      workingDir => {
        // extract dep jars
        for (artifact <- nonGatlingModules.map(_.jar)) {
          ZipUtil.unpack(artifact, workingDir, name => if (exclude(name)) null else name)
        }

        // copy compiled classes
        for (directory <- classDirectories if directory.exists) {
          val directoryPath = directory.toPath
          FileUtilsV2_2.copyDirectory(directory, workingDir, pathname => !exclude(directoryPath.relativize(pathname.toPath).toString), false)
        }

        // generate fake manifest
        val manifest = new File(new File(workingDir, "META-INF"), "MANIFEST.MF")
        manifest.getParentFile.mkdirs

        Using.fileWriter()(manifest)(_.write(s"""Manifest-Version: 1.0
                                                |Implementation-Title: ${rootModule.id.name}
                                                |Implementation-Version: ${rootModule.id.revision}
                                                |Specification-Vendor: $organisation
                                                |Implementation-Vendor: GatlingCorp
                                                |""".stripMargin))

        val fatjarArtifact = new File(target, fatJarName + ".jar")

        // generate jar
        ZipUtil.pack(workingDir, fatjarArtifact)

        fatjarArtifact
      },
      keepDirectory = false
    )
  }

  private def exclude(name: String): Boolean =
    name.equalsIgnoreCase("META-INF/LICENSE") ||
      name.equalsIgnoreCase("META-INF/MANIFEST.MF") ||
      name.endsWith(".SF") ||
      name.endsWith(".DSA") ||
      name.endsWith(".RSA")
}
