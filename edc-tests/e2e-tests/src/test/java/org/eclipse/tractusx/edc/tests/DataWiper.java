package org.eclipse.tractusx.edc.tests;

import org.eclipse.edc.connector.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.spi.asset.AssetIndex;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import java.util.stream.Collectors;

public class DataWiper {

    private final ServiceExtensionContext context;

    public DataWiper(ServiceExtensionContext context) {
        this.context = context;
    }

    public void clearPersistence() {
        clearAssetIndex();
        clearPolicies();
        clearContractDefinitions();
    }

    private void clearContractDefinitions() {
        var cds = context.getService(ContractDefinitionStore.class);
        cds.findAll(QuerySpec.none())
                .forEach(cd -> cds.deleteById(cd.getId()));
    }

    private void clearPolicies() {
        var ps = context.getService(PolicyDefinitionStore.class);
        ps.findAll(QuerySpec.none()).collect(Collectors.toList())
                .forEach(p -> ps.deleteById(p.getId()));
    }

    private void clearAssetIndex() {
        var index = context.getService(AssetIndex.class);
        index.queryAssets(QuerySpec.none())
                .forEach(asset -> index.deleteById(asset.getId()));
    }
}
