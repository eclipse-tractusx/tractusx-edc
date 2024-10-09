package org.eclipse.tractusx.edc.agreements.retirement.store;

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
import org.eclipse.tractusx.edc.agreements.retirement.spi.store.AgreementsRetirementStore;
import org.eclipse.tractusx.edc.agreements.retirement.store.sql.PostgresAgreementRetirementStatements;
import org.eclipse.tractusx.edc.agreements.retirement.store.sql.SqlAgreementsRetirementStatements;
import org.eclipse.tractusx.edc.agreements.retirement.store.sql.SqlAgreementsRetirementStore;

@Extension("Registers an SQL implementation for the ContractAgreementStore")
public class SqlAgreementsRetirementStoreExtension implements ServiceExtension {

    private static final String NAME = "SQL Agreement Retirement Store.";

    @Setting(value = "Datasource name for the SQL AgreementsRetirement store")
    private static final String DATASOURCE_SETTING_NAME = "edc.datasource.agreement.retirement.name";

    @Inject
    private DataSourceRegistry dataSourceRegistry;

    @Inject
    private TransactionContext transactionContext;

    @Inject
    private TypeManager typeManager;

    @Inject
    private QueryExecutor queryExecutor;

    @Inject
    private SqlAgreementsRetirementStatements statements;

    @Provider
    public AgreementsRetirementStore sqlStore(ServiceExtensionContext context) {
        var dataSourceName = context.getConfig().getString(DATASOURCE_SETTING_NAME, DataSourceRegistry.DEFAULT_DATASOURCE);
        return new SqlAgreementsRetirementStore(dataSourceRegistry, dataSourceName, transactionContext, typeManager.getMapper(), queryExecutor, getStatements());
    }

    @Override
    public String name() {
        return NAME;
    }

    private SqlAgreementsRetirementStatements getStatements() {
        return statements == null ? new PostgresAgreementRetirementStatements() : statements;
    }
}
