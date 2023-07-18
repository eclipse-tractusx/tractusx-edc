/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.helpers;

import org.postgresql.ds.PGSimpleDataSource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import javax.sql.DataSource;

import static java.lang.String.format;

@Deprecated(forRemoval = true)
public final class TxPostgresqlLocalInstance {
    private final String password;
    private final String jdbcUrlPrefix;
    private final String username;
    private final String databaseName;

    public TxPostgresqlLocalInstance(String user, String password, String jdbcUrlPrefix, String db) {
        username = user;
        this.password = password;
        this.jdbcUrlPrefix = jdbcUrlPrefix;
        databaseName = db;
    }

    public void createDatabase() {
        createDatabase(databaseName);
    }

    public void createDatabase(String name) {
        try (var connection = DriverManager.getConnection(jdbcUrlPrefix + username, username, password)) {
            connection.createStatement().execute(format("create database %s;", name));
        } catch (SQLException e) {
            e.printStackTrace();
            // database could already exist
        }
    }

    public Connection getTestConnection(String hostName, int port, String dbName) {
        try {
            return createTestDataSource(hostName, port, dbName).getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Connection getConnection() {
        try {
            return DriverManager.getConnection(jdbcUrlPrefix, username, password);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public String getJdbcUrlPrefix() {
        return jdbcUrlPrefix;
    }

    private DataSource createTestDataSource(String hostName, int port, String dbName) {
        var dataSource = new PGSimpleDataSource();
        dataSource.setServerNames(new String[]{ hostName });
        dataSource.setPortNumbers(new int[]{ port });
        dataSource.setUser(username);
        dataSource.setPassword(password);
        dataSource.setDatabaseName(dbName);
        return dataSource;
    }
}
