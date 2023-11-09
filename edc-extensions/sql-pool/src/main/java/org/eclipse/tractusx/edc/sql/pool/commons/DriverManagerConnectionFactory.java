/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.tractusx.edc.sql.pool.commons;

import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.sql.ConnectionFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Objects;
import java.util.Properties;

public class DriverManagerConnectionFactory implements ConnectionFactory {
    private final String jdbcUrl;
    private final Properties properties;

    public DriverManagerConnectionFactory(String jdbcUrl, Properties properties) {
        this.jdbcUrl = Objects.requireNonNull(jdbcUrl);
        this.properties = Objects.requireNonNull(properties);
    }

    @Override
    public Connection create() {
        try {
            return DriverManager.getConnection(jdbcUrl, properties);
        } catch (Exception exception) {
            throw new EdcPersistenceException(exception.getMessage(), exception);
        }
    }
}
