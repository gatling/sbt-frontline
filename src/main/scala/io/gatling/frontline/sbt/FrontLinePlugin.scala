/**
 * Copyright 2011-2019 GatlingCorp (http://gatling.io)
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

import io.gatling.frontline.sbt.FrontlineKeys._

import sbt.Keys._
import sbt._

object FrontlineKeys {
  val assembly = taskKey[File]("Builds a fatjar for FrontLine.")
  val assemblyJarName = taskKey[String]("name of the fat jar")
}
object FrontLinePlugin extends AutoPlugin {

  override def requires = plugins.JvmPlugin
  override def trigger = allRequirements

  val autoImport = FrontlineKeys

  override lazy val projectSettings = frontlineSettings(Test)

  def frontlineSettings(config: Configuration) =
    inConfig(config)(
      Seq(
        assembly := fatJarTask(config).value,
        assemblyJarName := s"${moduleName.value}-frontline-${version.value}"
      )
    )

  private def fatJarTask(config: Configuration) = Def.task {
    val moduleDescriptor = moduleDescriptorConfig.value

    val dependencies = DependencyFilter.nonGatlingDependencies(
      dependencyResolution.value,
      updateConfiguration.value,
      (unresolvedWarningConfiguration in update).value,
      config,
      streams.value.log,
      moduleDescriptor
    )

    val classesDirectories = (fullClasspath in config).value.map(_.data).filter(_.isDirectory)

    FatJar.packageFatJar(moduleDescriptor.module, classesDirectories, dependencies, target.value, assemblyJarName.value)
  }

  private def moduleDescriptorConfig = Def.task {
    moduleSettings.value match {
      case config: ModuleDescriptorConfiguration => config
      case x =>
        throw new IllegalStateException(s"sbt-frontline expected a ModuleDescriptorConfiguration, but got a ${x.getClass}")
    }
  }
}
