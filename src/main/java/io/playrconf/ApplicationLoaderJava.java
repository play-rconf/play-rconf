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
package io.playrconf;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.playrconf.sdk.Provider;
import play.Logger;
import play.LoggerConfigurator;
import play.inject.guice.GuiceApplicationBuilder;
import play.inject.guice.GuiceApplicationLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Extends the default application loader to inject the
 * remote configuration into the local configuration.
 *
 * @author Thibault Meyer
 * @since 18.04.01
 */
public class ApplicationLoaderJava extends GuiceApplicationLoader {

    /**
     * This is the base of all configuration keys used by
     * the project "Remote Configuration".
     */
    private static final String BASE_REMOTE_CONF_KEY = "remote-configuration.";

    /**
     * Logger.
     */
    private static final Logger.ALogger LOGGER = Logger.of("play-rconf");

    /**
     * Iterate over all declared providers.
     *
     * @param localConfiguration The local configuration
     * @return The configuration retrieved from all working providers
     */
    private Config processAllProviders(final Config localConfiguration) {
        final List<String> providerClassPaths = new ArrayList<>();
        if (localConfiguration.hasPath(BASE_REMOTE_CONF_KEY + "providers")) {
            switch (localConfiguration.getValue(BASE_REMOTE_CONF_KEY + "providers").valueType()) {
                case LIST: {
                    providerClassPaths.addAll(
                        localConfiguration
                            .getStringList(BASE_REMOTE_CONF_KEY + "providers")
                            .stream()
                            .map(String::trim)
                            .filter(cp -> !cp.isEmpty())
                            .collect(Collectors.toList())
                    );
                    break;
                }
                case STRING: {
                    final String cleanedClassPath = localConfiguration
                        .getString(BASE_REMOTE_CONF_KEY + "providers")
                        .trim();
                    if (!cleanedClassPath.isEmpty()) {
                        providerClassPaths.add(cleanedClassPath);
                    }
                    break;
                }
            }
        }
        if (localConfiguration.hasPath(BASE_REMOTE_CONF_KEY + "provider")) {
            final String cleanedClassPath = localConfiguration
                .getString(BASE_REMOTE_CONF_KEY + "provider")
                .trim();
            if (!cleanedClassPath.isEmpty()) {
                providerClassPaths.add(cleanedClassPath);
            }
        }
        if (!providerClassPaths.isEmpty()) {
            final StringBuilder sb = new StringBuilder(512);
            final AtomicInteger keyFetchCount = new AtomicInteger(0);
            final AtomicInteger storedFileCount = new AtomicInteger(0);

            providerClassPaths.forEach(classPath -> {
                try {
                    final Provider provider = Class
                        .forName(classPath)
                        .asSubclass(Provider.class)
                        .newInstance();
                    if (localConfiguration.hasPath(BASE_REMOTE_CONF_KEY + provider.getConfigurationObjectName())) {
                        provider.loadData(
                            localConfiguration.getConfig(BASE_REMOTE_CONF_KEY + provider.getConfigurationObjectName()),
                            kvObj -> {
                                kvObj.apply(sb);
                                keyFetchCount.incrementAndGet();
                                if (LOGGER.isDebugEnabled()) {
                                    if (kvObj.toString().contains("password")) {
                                        kvObj.setToStringWithMask(true);
                                    }
                                    LOGGER.debug("[{}] {}", provider.getName(), kvObj);
                                }
                            },
                            fileObj -> {
                                fileObj.apply();
                                storedFileCount.incrementAndGet();
                                if (LOGGER.isDebugEnabled()) {
                                    LOGGER.debug("[{}] Store {}", provider.getName(), fileObj);
                                }
                            }
                        );
                    }
                    LOGGER.info(
                        "[{}] {} configuration keys fetched and {} files stored",
                        provider.getName(),
                        keyFetchCount.get(),
                        storedFileCount.get()
                    );
                    keyFetchCount.set(0);
                    storedFileCount.set(0);
                } catch (final IllegalAccessException | InstantiationException ex) {
                    LOGGER.error("Can't instantiate the provider {}", classPath, ex);
                } catch (final ClassNotFoundException ignore) {
                    LOGGER.error("The provider {} does not exists", classPath);
                }
            });
            return ConfigFactory.parseString(sb.toString());
        }
        return ConfigFactory.empty();
    }

    @Override
    public GuiceApplicationBuilder builder(final play.ApplicationLoader.Context context) {
        // Initialize Logger
        LoggerConfigurator.apply(
            context.environment().classLoader()
        ).ifPresent(loggerConfigurator -> loggerConfigurator.configure(context.environment()));

        // Retrieve remote configuration and return builder
        final Config localConfiguration = context.initialConfig();
        return this.initialBuilder
            .in(context.environment())
            .loadConfig(
                this.processAllProviders(localConfiguration)
                    .withFallback(localConfiguration)
            )
            .overrides(overrides(context));
    }
}
