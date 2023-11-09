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

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.DestroyMode;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.sql.pool.ConnectionPool;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;
import javax.sql.DataSource;

public final class CommonsConnectionPool implements ConnectionPool, AutoCloseable {
    private final GenericObjectPool<Connection> connectionObjectPool;
    private final CommonsConnectionPoolConfig poolConfig;

    public CommonsConnectionPool(DataSource dataSource, CommonsConnectionPoolConfig commonsConnectionPoolConfig, Monitor monitor) {
        this.poolConfig = commonsConnectionPoolConfig;
        Objects.requireNonNull(dataSource, "connectionFactory");
        Objects.requireNonNull(commonsConnectionPoolConfig, "commonsConnectionPoolConfig");

        this.connectionObjectPool = new GenericObjectPool<>(
                new PooledConnectionObjectFactory(dataSource, commonsConnectionPoolConfig.getTestQuery(), monitor),
                getGenericObjectPoolConfig(commonsConnectionPoolConfig));
    }

    private static GenericObjectPoolConfig<Connection> getGenericObjectPoolConfig(CommonsConnectionPoolConfig commonsConnectionPoolConfig) {
        GenericObjectPoolConfig<Connection> genericObjectPoolConfig = new GenericObjectPoolConfig<>();

        // no need for JMX
        genericObjectPoolConfig.setJmxEnabled(false);

        genericObjectPoolConfig.setMaxIdle(commonsConnectionPoolConfig.getMaxIdleConnections());
        genericObjectPoolConfig.setMaxTotal(commonsConnectionPoolConfig.getMaxTotalConnections());
        genericObjectPoolConfig.setMinIdle(commonsConnectionPoolConfig.getMinIdleConnections());

        genericObjectPoolConfig.setTestOnBorrow(commonsConnectionPoolConfig.getTestConnectionOnBorrow());
        genericObjectPoolConfig.setTestOnCreate(commonsConnectionPoolConfig.getTestConnectionOnCreate());
        genericObjectPoolConfig.setTestOnReturn(commonsConnectionPoolConfig.getTestConnectionOnReturn());
        genericObjectPoolConfig.setTestWhileIdle(commonsConnectionPoolConfig.getTestConnectionWhileIdle());

        return genericObjectPoolConfig;
    }

    @Override
    public Connection getConnection() {
        try {
            return connectionObjectPool.borrowObject();
        } catch (Exception e) {
            throw new EdcPersistenceException(e.getMessage(), e);
        }
    }

    @Override
    public void returnConnection(Connection connection) {
        Objects.requireNonNull(connection, "connection");

        connectionObjectPool.returnObject(connection);
    }

    @Override
    public void close() {
        connectionObjectPool.close();
    }

    public CommonsConnectionPoolConfig getPoolConfig() {
        return poolConfig;
    }

    private static class PooledConnectionObjectFactory extends BasePooledObjectFactory<Connection> {
        private final String testQuery;
        private final DataSource dataSource;

        private final Monitor monitor;

        PooledConnectionObjectFactory(@NotNull DataSource dataSource, @NotNull String testQuery, Monitor monitor) {
            this.dataSource = Objects.requireNonNull(dataSource);
            this.testQuery = Objects.requireNonNull(testQuery);
            this.monitor = monitor;
        }

        @Override
        public Connection create() throws SQLException {
            return dataSource.getConnection();
        }

        @Override
        public boolean validateObject(PooledObject<Connection> pooledObject) {
            if (pooledObject == null) {
                return false;
            }

            Connection connection = pooledObject.getObject();
            if (connection == null) {
                return false;
            }

            return isConnectionValid(connection);
        }

        @Override
        public PooledObject<Connection> wrap(Connection connection) {
            return new DefaultPooledObject<>(connection);
        }

        @Override
        public void destroyObject(PooledObject<Connection> pooledObject, DestroyMode destroyMode) throws Exception {
            if (pooledObject == null) {
                return;
            }

            Connection connection = pooledObject.getObject();

            if (connection != null && !connection.isClosed()) {
                connection.close();
            }

            pooledObject.invalidate();
        }

        private boolean isConnectionValid(Connection connection) {
            try {
                if (connection.isClosed()) {
                    return false;
                }

                try (PreparedStatement preparedStatement = connection.prepareStatement(testQuery)) {
                    preparedStatement.execute();
                    return rollbackIfNeeded(connection);
                }
            } catch (Exception e) { // any exception thrown indicates invalidity of the connection
                return false;
            }
        }

        private boolean rollbackIfNeeded(Connection connection) {
            try {
                if (!connection.getAutoCommit()) {
                    connection.rollback();
                }
                return true;
            } catch (SQLException e) {
                monitor.debug("Failed to rollback transaction", e);
            }
            return false;
        }

    }
}
