/********************************************************************************
 * Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/

package org.eclipse.tractusx.edc.samples.multitenancy;

import org.eclipse.edc.boot.system.runtime.BaseRuntime;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Properties;

import static java.lang.ClassLoader.getSystemClassLoader;

public class MultiTenantRuntime extends BaseRuntime {

    private final @NotNull Monitor monitor = createMonitor();

    public static void main(String[] args) {
        var runtime = new MultiTenantRuntime();
        runtime.boot(false);
    }

    @Override
    public void boot(boolean shutdownHook) {
        loadTenantsConfig().getConfig("edc.tenants").partition().forEach(this::bootTenant);
    }

    private void bootTenant(Config tenantConfig) {
        var baseProperties = System.getProperties();
        tenantConfig.getRelativeEntries().forEach(System::setProperty);
        var classPathEntries = Arrays.stream(System.getProperty("java.class.path").split(File.pathSeparator))
                .map(this::toUrl)
                .toArray(URL[]::new);

        Thread runtimeThread = null;
        try (var classLoader = URLClassLoader.newInstance(classPathEntries, getSystemClassLoader())) {
            runtimeThread = new Thread(() -> {
                try {
                    Thread.currentThread().setContextClassLoader(classLoader);
                    super.boot(false);
                } catch (Exception e) {
                    throw new EdcException(e);
                }
            });

            monitor.info("Starting tenant " + tenantConfig.currentNode());
            runtimeThread.start();

            runtimeThread.join(20_000);

        } catch (InterruptedException e) {
            runtimeThread.interrupt();
            throw new EdcException(e);
        } catch (IOException e) {
            throw new EdcException(e);
        } finally {
            System.setProperties(baseProperties);
        }
    }

    @NotNull
    private Config loadTenantsConfig() {
        var tenantsPath = System.getProperty("edc.tenants.path");
        if (tenantsPath == null) {
            throw new EdcException("No edc.tenants.path mandatory property provided");
        }
        try (var is = Files.newInputStream(Path.of(tenantsPath))) {
            var properties = new Properties();
            properties.load(is);
            return ConfigFactory.fromProperties(properties);
        } catch (IOException e) {
            throw new EdcException(e);
        }
    }

    private URL toUrl(String entry) {
        try {
            return new File(entry).toURI().toURL();
        } catch (MalformedURLException e) {
            throw new EdcException(e);
        }
    }
}
