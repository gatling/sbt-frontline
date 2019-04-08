/**
  * Copyright 2011-2018 GatlingCorp (http://gatling.io)
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
package io.gatling.frontline.sbt

import java.io.File

import sbt.Keys._
import sbt._

object FrontLinePlugin extends AutoPlugin {

  override def requires = plugins.JvmPlugin

  override def trigger = allRequirements

  object autoImport {
    val assembly = taskKey[File]("Builds a fatjar for FrontLine.")
    val assemblyIvyReport =
      taskKey[File]("A task which returns the location of the ivy report file for test configuration.")
    val assemblyJarName = taskKey[String]("name of the fat jar")
  }

  import autoImport._

  override lazy val projectSettings: Seq[Def.Setting[_]] =
    Seq(Compile, Test).flatMap(ivyReportForConfig)

  private def ivyReportForConfig(config: Configuration) =
    inConfig(config)(
      Seq(
        assemblyIvyReport := {
          Def.task {
            val ivyConfig =
              ivyConfiguration.value.asInstanceOf[InlineIvyConfiguration]
            val org = projectID.value.organization
            val name = ivyModule.value.moduleSettings match {
              case ic: InlineConfiguration => ic.module.name
              case _ =>
                throw new IllegalStateException(
                  "sbt-frontline plugin currently only supports InlineConfiguration of ivy settings (the default in sbt)")
            }
            new File(ivyConfig.resolutionCacheDir.get, s"reports/$org-$name-${config.name}.xml")

          } dependsOn (compile in config)
        }.value,
        assembly := frontlineTask(config).value,
        assemblyJarName in assembly := s"${moduleName.value}-frontline-${version.value}"
      )
    )

  private def frontlineTask(config: Configuration): Def.Initialize[Task[File]] =
    Def.task {
      FrontLineFatJar.fatjar(
        (assemblyIvyReport in config).value,
        organization.value,
        target.value,
        (fullClasspath in config).value.files,
        (assemblyJarName in assembly).value
      )
    }
}
