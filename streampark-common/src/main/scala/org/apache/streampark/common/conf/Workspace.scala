/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.streampark.common.conf

import org.apache.streampark.common.enums.StorageType
import org.apache.streampark.common.util.{HdfsUtils, SystemPropertyUtils}

import java.net.URI


object Workspace {

  lazy val local: Workspace = Workspace.of(StorageType.LFS)

  lazy val remote: Workspace = Workspace.of(StorageType.HDFS)

  def of(storageType: StorageType): Workspace = Workspace(storageType)

}

case class Workspace(storageType: StorageType) {

  private[this] def getConfigValue[T](option: InternalOption): T = {
    val s = SystemPropertyUtils.get(option.key)
    val v = InternalConfigHolder.get(option).asInstanceOf[T]
    val d = option.defaultValue.asInstanceOf[T]
    (s, v) match {
      case (null, null) => d
      case (null, b) => b
      case (a, null) => Converter.convert(a, option.classType).asInstanceOf[T]
      case (a, b) => if (b == d) Converter.convert(a, option.classType).asInstanceOf[T] else b
    }
  }

  lazy val WORKSPACE: String = {
    storageType match {
      case StorageType.LFS =>
        val path: String = getConfigValue[String](CommonConfig.STREAMPARK_WORKSPACE_LOCAL)
        require(path != null, "[StreamPark] streampark.workspace.local must not be null")
        path
      case StorageType.HDFS =>
        val path: String = getConfigValue[String](CommonConfig.STREAMPARK_WORKSPACE_REMOTE)
        path match {
          case p if p.isEmpty =>
            s"${HdfsUtils.getDefaultFS}${CommonConfig.STREAMPARK_WORKSPACE_REMOTE.defaultValue}"
          case p =>
            val defaultFs = HdfsUtils.getDefaultFS
            if (p.startsWith("hdfs://")) {
              if (p.startsWith(defaultFs)) {
                p
              } else {
                val path = URI.create(p).getPath
                s"${HdfsUtils.getDefaultFS}$path"
              }
            } else {
              s"${HdfsUtils.getDefaultFS}$p"
            }
        }
    }
  }

  lazy val APP_PLUGINS = s"$WORKSPACE/plugins"

  lazy val APP_CLIENT = s"$WORKSPACE/client"

  /**
   * store flink multi version jars
   */
  lazy val APP_SHIMS = s"$WORKSPACE/shims"

  lazy val APP_UPLOADS = s"$WORKSPACE/uploads"

  lazy val APP_WORKSPACE = s"$WORKSPACE/workspace"

  lazy val APP_FLINK = s"$WORKSPACE/flink"

  lazy val APP_BACKUPS = s"$WORKSPACE/backups"

  /**
   * local build path
   */
  lazy val APP_LOCAL_DIST = s"$WORKSPACE/dist"

  lazy val APP_SAVEPOINTS = s"$WORKSPACE/savepoints"

  /**
   * store global public jars
   */
  lazy val APP_JARS = s"$WORKSPACE/jars"

  /**
   * dirpath of the maven local repository with built-in compilation process
   */
  lazy val MAVEN_LOCAL_DIR = s"${Workspace.local.WORKSPACE}/mvnrepo"

  /**
   * local sourceCode dir.(for git...)
   */
  lazy val PROJECT_LOCAL_DIR = s"${Workspace.local.WORKSPACE}/project"

}

