/*
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.tractusx.edc.validation.businesspartner.store;

import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.eclipse.tractusx.edc.validation.businesspartner.spi.BusinessPartnerStore;
import org.eclipse.tractusx.edc.validation.businesspartner.store.sql.BusinessPartnerGroupStatements;
import org.eclipse.tractusx.edc.validation.businesspartner.store.sql.PostgresBusinessPartnerGroupStatements;
import org.eclipse.tractusx.edc.validation.businesspartner.store.sql.SqlBusinessPartnerStore;

@Extension("Registers an SQL implementation for the BusinessPartnerGroupStore")
public class SqlBusinessPartnerGroupStoreExtension implements ServiceExtension {

    private static final String DEFAULT_DATASOURCE_NAME = "bpn";
    @Setting(value = "Datasource name for the SQL BusinessPartnerGroup store", defaultValue = DEFAULT_DATASOURCE_NAME)
    private static final String DATASOURCE_SETTING_NAME = "edc.datasource.bpn.name";
    private static final String NAME = "SQL Business Partner Store";
    @Inject
    private DataSourceRegistry dataSourceRegistry;
    @Inject
    private TransactionContext transactionContext;
    @Inject
    private TypeManager typeManager;
    @Inject
    private QueryExecutor queryExecutor;
    @Inject(required = false)
    private BusinessPartnerGroupStatements statements;

    @Provider
    public BusinessPartnerStore sqlStore(ServiceExtensionContext context) {
        var dataSourceName = context.getConfig().getString(DATASOURCE_SETTING_NAME, DEFAULT_DATASOURCE_NAME);
        return new SqlBusinessPartnerStore(dataSourceRegistry, dataSourceName, transactionContext, typeManager.getMapper(), queryExecutor, getStatements());
    }

    @Override
    public String name() {
        return NAME;
    }

    private BusinessPartnerGroupStatements getStatements() {
        return statements == null ? new PostgresBusinessPartnerGroupStatements() : statements;
    }
}
