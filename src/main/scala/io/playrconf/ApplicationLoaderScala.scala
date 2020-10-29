/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2018 The Play Remote Configuration Authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.playrconf

import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

import com.typesafe.config.{Config, ConfigFactory, ConfigValueType}
import io.playrconf.sdk.Provider
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceApplicationLoader}
import play.api.{Configuration, Logger, LoggerConfigurator}

import scala.jdk.CollectionConverters._

/**
  * Extends the default application loader to inject the
  * remote configuration into the local configuration.
  *
  * @author Thibault Meyer
  * @since 18.04.01
  */
class ApplicationLoaderScala extends GuiceApplicationLoader {

  /**
    * This is the base of all configuration keys used by
    * the project "Remote Configuration".
    */
  private val BASE_REMOTE_CONF_KEY: String = "remote-configuration."

  /**
    * Logger.
    */
  private val LOGGER: Logger = Logger.apply("play-rconf")

  override protected def builder(context: play.api.ApplicationLoader.Context): GuiceApplicationBuilder = {
    // Initialize Logger
    LoggerConfigurator(context.environment.classLoader)
      .foreach { _.configure(context.environment) }

    // Retrieve remote configuration and return builder
    val localConfiguration: Configuration = context.initialConfiguration
    initialBuilder
      .disableCircularProxies()
      .in(context.environment)
      .loadConfig(
        localConfiguration withFallback Configuration.apply(
          this.processAllProviders(localConfiguration.underlying)
        )
      )
      .overrides(overrides(context): _*)
  }

  /**
    * Iterate over all declared providers.
    *
    * @param localConfiguration The local configuration
    * @return The configuration retrieved from all working providers
    */
  private[this] def processAllProviders(localConfiguration: Config): Config = {
    var providerClassPaths: List[String] = List[String]()

    if (localConfiguration.hasPath(BASE_REMOTE_CONF_KEY + "providers")) {
      localConfiguration.getValue(BASE_REMOTE_CONF_KEY + "providers").valueType() match {
        case ConfigValueType.LIST =>
          providerClassPaths ++= localConfiguration
            .getStringList(BASE_REMOTE_CONF_KEY + "providers")
            .asScala
            .to(LazyList)
            .map(_.trim)
            .filter(_.nonEmpty)
            .toList
        case ConfigValueType.STRING =>
          val cleanedClassPath: String = localConfiguration
            .getString(BASE_REMOTE_CONF_KEY + "providers")
            .trim
          if (cleanedClassPath.nonEmpty) {
            providerClassPaths ++= List(cleanedClassPath)
          }
        case _ => None
      }
    }
    if (localConfiguration.hasPath(BASE_REMOTE_CONF_KEY + "provider")) {
      val cleanedClassPath: String = localConfiguration
        .getString(BASE_REMOTE_CONF_KEY + "provider")
        .trim
      if (cleanedClassPath.nonEmpty) {
        providerClassPaths ++= List(cleanedClassPath)
      }
    }

    if (providerClassPaths.nonEmpty) {
      val sb: java.lang.StringBuilder = new java.lang.StringBuilder(512)
      val keyFetchCount: AtomicInteger = new AtomicInteger(0)
      val storedFileCount: AtomicInteger = new AtomicInteger(0)

      providerClassPaths.foreach(classPath => {
        try {
          val provider: Provider = Class
            .forName(classPath)
            .newInstance
            .asInstanceOf[Provider]
          if (localConfiguration.hasPath(BASE_REMOTE_CONF_KEY + provider.getConfigurationObjectName)) {
            provider.loadData(
              localConfiguration.getConfig(BASE_REMOTE_CONF_KEY + provider.getConfigurationObjectName),
              kvObj => {
                kvObj.apply(sb)
                keyFetchCount.incrementAndGet()
                if (LOGGER.isDebugEnabled) {
                  val objString: String = kvObj.toString.toLowerCase(Locale.ENGLISH)
                  if (objString.contains("password") || objString.contains("secret")) {
                    kvObj.setToStringWithMask(true)
                  }
                  LOGGER.debug(s"[${provider.getName}] $kvObj")
                }
              },
              fileObj => {
                fileObj.apply()
                storedFileCount.incrementAndGet()
                if (LOGGER.isDebugEnabled) {
                  LOGGER.debug(s"[${provider.getName}] $fileObj")
                }
              }
            )
          }
          LOGGER.info(
            s"[${provider.getName}] ${keyFetchCount.get} configuration keys fetched and ${storedFileCount.get} files stored"
          )
          keyFetchCount.set(0)
          storedFileCount.set(0)
        } catch {
          case _: ClassNotFoundException =>
            LOGGER.error(s"The provider $classPath does not exists")
          case ex: IllegalAccessException =>
            LOGGER.error(s"Can't instantiate the provider $classPath", ex)
          case ex: InstantiationException =>
            LOGGER.error(s"Can't instantiate the provider $classPath", ex)
        }
      })
      return ConfigFactory.parseString(sb.toString)
    }

    ConfigFactory.empty()
  }
}
