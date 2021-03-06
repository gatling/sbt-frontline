/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.gatling.frontline.sbt

import java.io.File

import org.zeroturnaround.zip.ZipUtil
import org.zeroturnaround.zip.commons.FileUtilsV2_2

import sbt._
import sbt.io.IO
import sbt.librarymanagement.ModuleID

object FatJar {

  def packageFatJar(rootModule: ModuleID, classesDirectory: Seq[File], gatlingVersion: String, dependencies: Seq[File], target: File, jarName: String): File =
    IO.withTemporaryDirectory(
      workingDir => {
        extractDependencies(workingDir, dependencies)
        copyClasses(workingDir, classesDirectory)
        generateManifest(workingDir, rootModule, gatlingVersion)

        val fatJarFile = target / s"$jarName.jar"
        // Generate fatjar
        ZipUtil.pack(workingDir, fatJarFile)
        fatJarFile
      },
      keepDirectory = false
    )

  private def extractDependencies(workingDir: File, dependencies: Seq[File]): Unit =
    dependencies.foreach(dep => ZipUtil.unpack(dep, workingDir, name => if (isExcluded(name)) null else name))

  private def copyClasses(workingDir: File, classesDirectories: Seq[File]): Unit =
    classesDirectories.filter(_.exists()).foreach { directory =>
      val directoryPath = directory.toPath
      FileUtilsV2_2.copyDirectory(directory, workingDir, pathname => !isExcluded(directoryPath.relativize(pathname.toPath).toString), false)
    }

  private def generateManifest(workingDir: File, rootModule: ModuleID, gatlingVersion: String): Unit = {
    val manifest = s"""Manifest-Version: 1.0
                      |Implementation-Title: ${rootModule.name}
                      |Implementation-Vendor: ${rootModule.organization}
                      |Implementation-Version: ${rootModule.revision}
                      |Specification-Vendor: GatlingCorp
                      |Gatling-Version: $gatlingVersion
                      |""".stripMargin

    IO.write(workingDir / "META-INF" / "MANIFEST.MF", manifest)
  }

  private def isExcluded(name: String): Boolean =
    name.equalsIgnoreCase("META-INF/LICENSE") ||
      name.equalsIgnoreCase("META-INF/MANIFEST.MF") ||
      name.startsWith("META-INF/versions/") ||
      name.endsWith(".SF") ||
      name.endsWith(".DSA") ||
      name.endsWith(".RSA")
}
