/*
 * Copyright (c) 2025 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

import org.apache.logging.log4j.Level;
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

    @Override
    public void severe(final Supplier<String> supplier, final Throwable... errors) {
        log(supplier, Level.ERROR, errors);
    }

    @Override
    public void warning(final Supplier<String> supplier, final Throwable... errors) {
        log(supplier, Level.WARN, errors);
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
        if (errors == null || errors.length == 0) {
            LOGGER.log(level, () -> sanitizeMessage(supplier));
        } else {
            Arrays.stream(errors).forEach(error -> LOGGER.log(level, sanitizeMessage(supplier), error));
        }
    }
}