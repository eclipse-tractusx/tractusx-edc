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

package org.eclipse.tractusx.edc.slf4jlogger;

import org.eclipse.edc.spi.monitor.Monitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.function.Supplier;


// to be extended
public class Slf4jLoggerMonitor implements Monitor {
    private static final Logger LOGGER = LoggerFactory.getLogger(Slf4jLoggerMonitor.class);

    @Override
    public void info(String message, Throwable... errors) {
        LOGGER.info(message, errors);
    }

    @Override
    public void severe(String message, Throwable... errors) {
        LOGGER.error(message, errors);
    }

    public void severe(Supplier<String> supplier, Throwable... errors) {
        this.severe(supplier.get(), errors);
    }

    @Override
    public void severe(Map<String, Object> data) {

    }

    public void warning(Supplier<String> supplier, Throwable... errors) {
        this.warning(supplier.get(), errors);
    }

    public void warning(String message, Throwable... errors) {
        LOGGER.warn(message, errors);
    }

    public void info(Supplier<String> supplier, Throwable... errors) {
        this.info(supplier.get(), errors);
    }

    public void debug(Supplier<String> supplier, Throwable... errors) {
        this.debug(supplier.get(), errors);
    }

    public void debug(String message, Throwable... errors) {
        LOGGER.debug(message, errors);
    }
}