/********************************************************************************
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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
 ********************************************************************************/

package org.eclipse.tractusx.edc.tests.runtimes;

import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.edr.spi.store.EndpointDataReferenceStore;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.tractusx.edc.validation.businesspartner.spi.store.BusinessPartnerStore;

import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.CONSUMER_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_BPN;

/**
 * Helper class to delete all objects from a runtime's data stores.
 */
public class DataWiper {

    protected final ServiceExtensionContext context;

    public DataWiper(ServiceExtensionContext context) {
        this.context = context;
    }

    public void clearPersistence() {
        clearAssetIndex();
        clearPolicies();
        clearContractDefinitions();
        clearEdrCache();
        clearBusinessPartnerStore();
    }

    public void clearBusinessPartnerStore() {
        var bps = context.getService(BusinessPartnerStore.class);
        bps.delete(CONSUMER_BPN);
        bps.delete(PROVIDER_BPN);
    }

    public void clearContractDefinitions() {
        var cds = context.getService(ContractDefinitionStore.class);
        cds.findAll(QuerySpec.max()).forEach(cd -> cds.deleteById(cd.getId()));
    }

    public void clearPolicies() {
        var ps = context.getService(PolicyDefinitionStore.class);
        // must .collect() here, otherwise we'll get a ConcurrentModificationException
        ps.findAll(QuerySpec.max()).toList().forEach(p -> ps.delete(p.getId()));
    }

    public void clearAssetIndex() {
        var index = context.getService(AssetIndex.class);
        index.queryAssets(QuerySpec.max()).forEach(asset -> index.deleteById(asset.getId()));
    }

    public void clearEdrCache() {
        var edrCache = context.getService(EndpointDataReferenceStore.class);
        edrCache.query(QuerySpec.max()).getContent().forEach(entry -> {
            try {
                edrCache.delete(entry.getTransferProcessId());
            } catch (Exception e) {
                context.getMonitor().warning("Failed to clean up the cache", e);
            }
        });
    }
}
