/*
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft
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
 */

package org.eclipse.tractusx.edc.core.utils;

import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.jetbrains.annotations.NotNull;

public class ConfigUtil {

    public static final String DEPRECATION_WARNING = "Deprecated property used: '%s'. Please consider using the new property '%s' instead.";

    public static void missingMandatoryProperty(Monitor monitor, String missingConfig) {
        throw new EdcException("Mandatory config value missing: '%s'. This runtime is not operational.".formatted(missingConfig));
    }

    public static String propertyCompatibility(ServiceExtensionContext context, String config, String deprecatedConfig, String defaultValue) {
        var value = context.getSetting(config, null);
        if (value == null) {
            value = context.getSetting(deprecatedConfig, null);
            if (value == null) {
                return defaultValue;
            }
            context.getMonitor().warning(DEPRECATION_WARNING.formatted(deprecatedConfig, config));
        }
        return value;
    }

    public static int propertyCompatibility(ServiceExtensionContext context, String config, String deprecatedConfig, int defaultValue) {
        var value = context.getConfig().getInteger(config, null);
        if (value == null) {
            value = context.getConfig().getInteger(deprecatedConfig, null);
            if (value == null) {
                return defaultValue;
            }
            context.getMonitor().warning(DEPRECATION_WARNING.formatted(deprecatedConfig, config));
        }
        return value;
    }

    public static long propertyCompatibility(ServiceExtensionContext context, String config, String deprecatedConfig, long defaultValue) {
        var value = context.getConfig().getLong(config, null);
        if (value == null) {
            value = context.getConfig().getLong(deprecatedConfig, null);
            if (value == null) {
                return defaultValue;
            }
            context.getMonitor().warning(DEPRECATION_WARNING.formatted(deprecatedConfig, config));
        }
        return value;
    }

    @NotNull
    public static String propertyCompatibility(ServiceExtensionContext context, String config, String deprecatedConfig) {
        var value = context.getSetting(config, null);
        if (value == null) {
            value = context.getConfig().getString(deprecatedConfig);
            context.getMonitor().warning(DEPRECATION_WARNING.formatted(deprecatedConfig, config));
        }
        return value;
    }

    public static String propertyCompatibilityNullable(ServiceExtensionContext context, String config, String deprecatedConfig) {
        var value = context.getSetting(config, null);
        if (value == null) {
            value = context.getConfig().getString(deprecatedConfig, null);
            if (value != null) {
                context.getMonitor().warning(DEPRECATION_WARNING.formatted(deprecatedConfig, config));
            }
        }
        return value;
    }

    public static boolean propertyCompatibility(ServiceExtensionContext context, String config, String deprecatedConfig, boolean defaultValue) {
        var value = context.getConfig().getBoolean(config, null);
        if (value == null) {
            value = context.getConfig().getBoolean(deprecatedConfig, null);
            if (value == null) {
                return defaultValue;
            }
            context.getMonitor().warning(DEPRECATION_WARNING.formatted(deprecatedConfig, config));
        }
        return value;
    }
}
