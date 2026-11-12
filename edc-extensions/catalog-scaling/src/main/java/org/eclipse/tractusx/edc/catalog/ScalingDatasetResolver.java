/*
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 * Copyright (c) 2026 Volkswagen AG
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

package org.eclipse.tractusx.edc.catalog;

import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.catalog.spi.Catalog;
import org.eclipse.edc.connector.controlplane.catalog.spi.DataService;
import org.eclipse.edc.connector.controlplane.catalog.spi.Dataset;
import org.eclipse.edc.connector.controlplane.catalog.spi.DatasetResolver;
import org.eclipse.edc.connector.controlplane.catalog.spi.DistributionResolver;
import org.eclipse.edc.connector.controlplane.contract.spi.ContractOfferId;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.PolicyType;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.query.SortOrder;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.sql.DataSource;

import static org.eclipse.edc.participantcontext.spi.types.ParticipantResource.filterByParticipantContextId;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

//NOTICE: the following file has been used as a baseline: https://github.com/eclipse-edc/Connector/blob/v0.17.0/core/control-plane/control-plane-catalog/src/main/java/org/eclipse/edc/connector/controlplane/catalog/DatasetResolverImpl.java

/**
 * Generates datasets from available assets and contract definitions for a given requesting
 * participant. This implementation relies on Postgres to perform asset and contract definition
 * matching, which allows it handle larger numbers of assets/contract definition in comparison to
 * the upstream implementation.
 */
public class ScalingDatasetResolver implements DatasetResolver {

    private final ScalingContractDefinitionResolver contractDefinitionResolver;
    private final CatalogRequestQueryManager queryManager;
    private final PolicyDefinitionStore policyDefinitionStore;
    private final DistributionResolver distributionResolver;
    private final Supplier<DataSource> dataSourceSupplier;
    private final TransactionContext transactionContext;

    public ScalingDatasetResolver(ScalingContractDefinitionResolver contractDefinitionResolver,
                                  CatalogRequestQueryManager queryManager, PolicyDefinitionStore policyDefinitionStore,
                                  DistributionResolver distributionResolver, Supplier<DataSource> dataSourceSupplier,
                                  TransactionContext transactionContext) {
        this.contractDefinitionResolver = contractDefinitionResolver;
        this.queryManager = queryManager;
        this.policyDefinitionStore = policyDefinitionStore;
        this.distributionResolver = distributionResolver;
        this.dataSourceSupplier = dataSourceSupplier;
        this.transactionContext = transactionContext;
    }

    /**
     * Generates datasets for the requesting participant matching the given query. First resolves
     * all contract definitions for which the requesting participant is authorized and copied them
     * to a temporary Postgres table. Then queries for assets both matching the given query
     * and matching at least one contract definition in the temporary table. For all returned
     * assets, builds a dataset using policies from all authorized contract definitions matching the
     * asset.
     *
     * @param participantContext participant context id
     * @param agent the requesting participant agent
     * @param querySpec asset query
     * @param protocol protocol through which the catalog was requested
     * @return a stream of datasets for the requesting participant
     */
    @Override
    public @NotNull Stream<Dataset> query(ParticipantContext participantContext, ParticipantAgent agent, QuerySpec querySpec, String protocol) {
        return transactionContext.execute(() -> {
            var contractDefinitions = contractDefinitionResolver.resolveFor(participantContext, agent);
            if (contractDefinitions.isEmpty()) {
                return Stream.empty();
            }

            try {
                var connection = dataSourceSupplier.get().getConnection();
                var tempTableName = prepareTemporaryContractDefinitionTable(connection, participantContext, contractDefinitions);

                var assetsQuery = QuerySpec.Builder.newInstance()
                        .offset(querySpec.getOffset())
                        .limit(querySpec.getLimit())
                        .filter(querySpec.getFilterExpression())
                        .filter(filterByParticipantContextId(participantContext.getParticipantContextId()))
                        // the asset ID is unique, so sorting by it yields a total order and stable pages
                        .sortField("id")
                        .sortOrder(SortOrder.ASC)
                        .build();

                var contractPolicies = new HashMap<String, Policy>();
                List<Dataset> datasets;
                try (var assets = queryManager.queryAssetsWithMatchingContractDefinitions(connection, tempTableName, assetsQuery)) {
                    datasets = assets.map(asset -> toDataset(asset, contractDefinitions, protocol, contractPolicies)).toList();
                }

                return datasets.stream();
            } catch (Exception e) {
                throw new EdcException("Error querying datasets.", e);
            }
        });
    }

    /**
     * Generates a dataset for a specific requested asset. First resolves all contract definitions
     * for which the requesting participant is authorized and copied them to a temporary Postgres
     * table. Then queries for the asset by id including IDs of all matching contract definitions
     * in the temporary table. If the asset is found and matches at least one authorized contract
     * definition, builds a dataset using policies from all authorized contract definitions matching
     * the asset.
     *
     * @param participantContext participant context id
     * @param agent the requesting participant agent
     * @param id id of the asset
     * @param protocol protocol through which the catalog was requested
     * @return a dataset for the requesting asset, or null if the asset is not found or not available to the requesting participant
     */
    @Override
    public Dataset getById(ParticipantContext participantContext, ParticipantAgent agent, String id, String protocol) {
        return transactionContext.execute(() -> {
            var contractDefinitions = contractDefinitionResolver.resolveFor(participantContext, agent);
            if (contractDefinitions.isEmpty()) {
                return null;
            }

            try {
                var connection = dataSourceSupplier.get().getConnection();
                var tempTableName = prepareTemporaryContractDefinitionTable(connection, participantContext, contractDefinitions);

                return queryManager.findAssetWithMatchingContractDefinitions(connection, tempTableName, id)
                        .map(asset -> toDataset(asset, contractDefinitions, protocol, new HashMap<>()))
                        .filter(Dataset::hasOffers)
                        .orElse(null);
            } catch (Exception e) {
                throw new EdcException("Error getting dataset by id.", e);
            }
        });
    }

    private String prepareTemporaryContractDefinitionTable(Connection connection, ParticipantContext participantContext,
                                                           Map<String, ContractDefinition> contractDefinitions) {
        var tempTableName = queryManager.createTemporaryContractDefinitionTable(connection);
        queryManager.copyAuthorizedContractDefinitions(connection, participantContext.getParticipantContextId(),
                tempTableName, List.copyOf(contractDefinitions.keySet()));
        queryManager.runAnalyze(connection, tempTableName);
        return tempTableName;
    }

    private Dataset.Builder<?, ?> buildDataset(Asset asset) {
        if (!asset.isCatalog()) {
            return Dataset.Builder.newInstance();
        }

        return Catalog.Builder.newInstance()
                .dataService(DataService.Builder.newInstance()
                        .id(Base64.getUrlEncoder().encodeToString(asset.getId().getBytes()))
                        .endpointDescription(asset.getDescription())
                        .endpointUrl(asset.getDataAddress().getStringProperty(EDC_NAMESPACE + "baseUrl", null))
                        .build());
    }

    private Dataset toDataset(AuthorizedAsset authorizedAsset, Map<String, ContractDefinition> contractDefinitionsById,
                              String protocol, Map<String, Policy> policies) {
        var asset = authorizedAsset.asset();

        var distributions = distributionResolver.getDistributions(protocol, asset);
        var datasetBuilder = buildDataset(asset)
                .id(asset.getId())
                .distributions(distributions)
                .properties(asset.getProperties());

        authorizedAsset.contractDefinitionIds().stream()
                .map(contractDefinitionsById::get)
                .filter(Objects::nonNull)
                .forEach(contractDefinition -> {
                    var policy = policies.computeIfAbsent(contractDefinition.getContractPolicyId(),
                            policyId -> Optional.ofNullable(policyDefinitionStore.findById(policyId))
                                    .map(PolicyDefinition::getPolicy)
                                    .orElse(null));

                    if (policy != null) {
                        var contractId = ContractOfferId.create(contractDefinition.getId(), asset.getId());
                        var offerPolicy = policy.toBuilder().type(PolicyType.OFFER).build();
                        datasetBuilder.offer(contractId.toString(), offerPolicy);
                    }
                });

        return datasetBuilder.build();
    }
}
