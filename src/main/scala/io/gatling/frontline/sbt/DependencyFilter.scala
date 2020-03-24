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

import java.io.File

import scala.annotation.tailrec
import scala.collection.mutable

import sbt.Keys.Classpath
import sbt.internal.util.Attributed
import sbt.librarymanagement._
import sbt.util.Logger

object DependencyFilter {

  private val GatlingOrgs = Set("io.gatling", "io.gatling.highcharts", "io.gatling.frontline")

  def nonGatlingDependencies(
      resolution: DependencyResolution,
      updateConfig: UpdateConfiguration,
      unresolvedWarningConfig: UnresolvedWarningConfiguration,
      config: Configuration,
      logger: Logger,
      rootModule: ModuleDescriptorConfiguration
  ): Vector[File] = {
    val moduleDescriptor = resolution.moduleDescriptor(rootModule)

    val updateReport = resolution
      .update(moduleDescriptor, updateConfig, unresolvedWarningConfig, logger)
      .getOrElse(throw new IllegalStateException("Cannot build a fatjar with unresolved dependencies"))

    val configurationReport = updateReport
      .configuration(ConfigRef.configToConfigRef(config))
      .getOrElse(throw new IllegalStateException(s"Could not find a report for configuration $config"))

    val callers = moduleCallers(configurationReport.modules)
    val excludedDeps = configurationReport.modules.filterNot(isTransitiveGatlingDependency(_, callers))

    excludedDeps
      .flatMap(_.artifacts)
      .collect { case (artifact, file) if artifact.`type` == Artifact.DefaultType || artifact.`type` == "bundle" => file }
  }

  private def moduleCallers(reports: Vector[ModuleReport]): Map[ModuleID, List[ModuleID]] =
    reports.map(report => (report.module.withConfigurations(None), report.callers.map(_.caller).toList)).toMap

  private def isTransitiveGatlingDependency(report: ModuleReport, callers: Map[ModuleID, List[ModuleID]]): Boolean = {
    @tailrec
    def isTransitiveGatlingDependencyRec(toCheck: List[ModuleID]): Boolean =
      toCheck match {
        case Nil                                                => false
        case dep :: _ if GatlingOrgs.contains(dep.organization) => true
        case dep :: rest                                        => isTransitiveGatlingDependencyRec(callers.getOrElse(dep, Nil) ::: rest)
      }

    isTransitiveGatlingDependencyRec(List(report.module.withConfigurations(None)))
  }
}
