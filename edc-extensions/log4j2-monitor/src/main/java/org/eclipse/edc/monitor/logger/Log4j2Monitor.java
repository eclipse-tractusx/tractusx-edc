/*
 * Copyright (c) 2025 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 * Copyright (c) 2026 Cofinity-X GmbH
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

package org.eclipse.edc.monitor.logger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.edc.spi.monitor.Monitor;

import java.util.Arrays;
import java.util.function.Supplier;

/**
 * Monitor that wraps the Log4J2 API.
 */
public class Log4j2Monitor implements Monitor {

    /**
     * Global logger.
     */
    private static final Logger LOGGER = LogManager.getLogger(Log4j2Monitor.class.getName());

    private final Level minLevel;

    public Log4j2Monitor() {
        this(Level.getDefaultLevel());
    }

    public Log4j2Monitor(Level level) {
        this.minLevel = level;
    }

    @Override
    public void severe(final Supplier<String> supplier, final Throwable... errors) {
        log(supplier, Level.SEVERE, errors);
    }

    @Override
    public void warning(final Supplier<String> supplier, final Throwable... errors) {
        log(supplier, Level.WARNING, errors);
    }

    @Override
    public void info(final Supplier<String> supplier, final Throwable... errors) {
        log(supplier, Level.INFO, errors);
    }

    @Override
    public void debug(final Supplier<String> supplier, final Throwable... errors) {
        log(supplier, Level.DEBUG, errors);
    }

    private void log(final Supplier<String> supplier, final Level level, final Throwable... errors) {
        if (level.value() < minLevel.value()) {
            return;
        }
        if (errors == null || errors.length == 0) {
            LOGGER.log(levelConverter(level), () -> sanitizeMessage(supplier));
        } else {
            Arrays.stream(errors).forEach(error -> LOGGER.log(levelConverter(level), sanitizeMessage(supplier), error));
        }
    }

    private org.apache.logging.log4j.Level levelConverter(Level level) {
        return switch (level) {
            case SEVERE -> org.apache.logging.log4j.Level.ERROR;
            case WARNING -> org.apache.logging.log4j.Level.WARN;
            case INFO -> org.apache.logging.log4j.Level.INFO;
            case DEBUG -> org.apache.logging.log4j.Level.DEBUG;
        };
    }
}