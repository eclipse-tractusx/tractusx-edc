/*
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

import javax.sql.DataSource;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.catalog.spi.Dataset;
import org.eclipse.edc.connector.controlplane.catalog.spi.DistributionResolver;
import org.eclipse.edc.connector.controlplane.contract.spi.ContractOfferId;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.query.SortOrder;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.eclipse.edc.transaction.spi.TransactionContext.ResultTransactionBlock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.participantcontext.spi.types.ParticipantResource.filterByParticipantContextId;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class ScalingDatasetResolverTest {

    private static final String PARTICIPANT_CONTEXT_ID = "participant-context";
    private static final String TEMP_TABLE = "edc_tmp_cd_test";
    private static final String PROTOCOL = "protocol";
    private static final String ASSET_ID = "asset";
    private static final String DEFINITION_ID_1 = "definition";
    private static final String DEFINITION_ID_2 = "other-definition";
    private static final String CONTRACT_POLICY_ID = "contract-policy";

    private final ScalingContractDefinitionResolver contractDefinitionResolver = mock();
    private final CatalogRequestQueryManager queryManager = mock();
    private final PolicyDefinitionStore policyDefinitionStore = mock();
    private final DistributionResolver distributionResolver = mock();
    private final DataSource dataSource = mock();
    private final Connection connection = mock();
    private final TransactionContext transactionContext = mock();

    private final ParticipantContext participantContext = ParticipantContext.Builder.newInstance()
            .participantContextId(PARTICIPANT_CONTEXT_ID)
            .identity("identity")
            .build();
    private final ParticipantAgent agent = new ParticipantAgent("agent", Map.of(), Map.of());

    private ScalingDatasetResolver resolver;

    @BeforeEach
    void setUp() throws SQLException {
        when(transactionContext.execute(any(ResultTransactionBlock.class)))
                .thenAnswer(invocation -> invocation.<ResultTransactionBlock<?>>getArgument(0).execute());
        when(dataSource.getConnection()).thenReturn(connection);
        when(queryManager.createTemporaryContractDefinitionTable(connection)).thenReturn(TEMP_TABLE);
        when(distributionResolver.getDistributions(eq(PROTOCOL), any())).thenReturn(List.of());
        when(policyDefinitionStore.findById(CONTRACT_POLICY_ID))
                .thenReturn(PolicyDefinition.Builder.newInstance().id(CONTRACT_POLICY_ID).policy(Policy.Builder.newInstance().build()).build());

        resolver = new ScalingDatasetResolver(contractDefinitionResolver, queryManager, policyDefinitionStore,
                distributionResolver, () -> dataSource, transactionContext);
    }

    @Test
    void query_shouldReuseConnectionAndTempTableForAllSteps() throws SQLException {
        when(contractDefinitionResolver.resolveFor(participantContext, agent)).thenReturn(authorizedDefinitions());
        when(queryManager.queryAssetsWithMatchingContractDefinitions(any(), anyString(), any()))
                .thenReturn(Stream.of(new AuthorizedAsset(asset(), List.of(DEFINITION_ID_1))));
        var querySpec = QuerySpec.Builder.newInstance()
                .offset(5)
                .limit(10)
                .filter(new Criterion("key", "=", "value"))
                .build();

        var datasets = resolver.query(participantContext, agent, querySpec, PROTOCOL).toList();

        assertThat(datasets).singleElement().satisfies(dataset -> {
            assertThat(dataset.getId()).isEqualTo(ASSET_ID);
            assertThat(offeredDefinitionIds(dataset)).containsExactly(DEFINITION_ID_1);
        });

        var assetsQuery = ArgumentCaptor.forClass(QuerySpec.class);
        var inOrder = inOrder(dataSource, queryManager);
        inOrder.verify(dataSource).getConnection();
        inOrder.verify(queryManager).createTemporaryContractDefinitionTable(connection);
        inOrder.verify(queryManager).copyAuthorizedContractDefinitions(connection, PARTICIPANT_CONTEXT_ID, TEMP_TABLE,
                List.of(DEFINITION_ID_1, DEFINITION_ID_2));
        inOrder.verify(queryManager).runAnalyze(connection, TEMP_TABLE);
        inOrder.verify(queryManager).queryAssetsWithMatchingContractDefinitions(eq(connection), eq(TEMP_TABLE), assetsQuery.capture());
        verifyNoMoreInteractions(dataSource, queryManager);

        assertThat(assetsQuery.getValue()).satisfies(query -> {
            assertThat(query.getOffset()).isEqualTo(5);
            assertThat(query.getLimit()).isEqualTo(10);
            assertThat(query.getFilterExpression()).containsExactly(new Criterion("key", "=", "value"),
                    filterByParticipantContextId(PARTICIPANT_CONTEXT_ID));
            assertThat(query.getSortField()).isEqualTo("id");
            assertThat(query.getSortOrder()).isEqualTo(SortOrder.ASC);
        });
    }

    @Test
    void query_shouldReturnEmptyStream_whenNoAssetIsFound() throws SQLException {
        when(contractDefinitionResolver.resolveFor(participantContext, agent)).thenReturn(authorizedDefinitions());
        when(queryManager.queryAssetsWithMatchingContractDefinitions(any(), anyString(), any()))
                .thenReturn(Stream.empty());

        var datasets = resolver.query(participantContext, agent, QuerySpec.max(), PROTOCOL);

        assertThat(datasets).isEmpty();

        var inOrder = inOrder(dataSource, queryManager);
        inOrder.verify(dataSource).getConnection();
        inOrder.verify(queryManager).createTemporaryContractDefinitionTable(connection);
        inOrder.verify(queryManager).copyAuthorizedContractDefinitions(connection, PARTICIPANT_CONTEXT_ID, TEMP_TABLE,
                List.of(DEFINITION_ID_1, DEFINITION_ID_2));
        inOrder.verify(queryManager).runAnalyze(connection, TEMP_TABLE);
        inOrder.verify(queryManager).queryAssetsWithMatchingContractDefinitions(eq(connection), eq(TEMP_TABLE), any());
        verifyNoMoreInteractions(dataSource, queryManager);
    }

    @Test
    void query_shouldNotQueryDatabase_whenNoContractDefinitionIsAuthorized() {
        when(contractDefinitionResolver.resolveFor(participantContext, agent)).thenReturn(Map.of());

        var datasets = resolver.query(participantContext, agent, QuerySpec.max(), PROTOCOL);

        assertThat(datasets).isEmpty();
        verifyNoInteractions(dataSource, queryManager);
    }

    @Test
    void getById_shouldReuseConnectionAndTempTableForAllSteps() throws SQLException {
        when(contractDefinitionResolver.resolveFor(participantContext, agent)).thenReturn(authorizedDefinitions());
        when(queryManager.findAssetWithMatchingContractDefinitions(any(), anyString(), anyString()))
                .thenReturn(Optional.of(new AuthorizedAsset(asset(), List.of(DEFINITION_ID_1, DEFINITION_ID_2))));

        var dataset = resolver.getById(participantContext, agent, ASSET_ID, PROTOCOL);

        assertThat(dataset).isNotNull();
        assertThat(dataset.getId()).isEqualTo(ASSET_ID);
        assertThat(offeredDefinitionIds(dataset)).containsExactlyInAnyOrder(DEFINITION_ID_1, DEFINITION_ID_2);

        var inOrder = inOrder(dataSource, queryManager);
        inOrder.verify(dataSource).getConnection();
        inOrder.verify(queryManager).createTemporaryContractDefinitionTable(connection);
        inOrder.verify(queryManager).copyAuthorizedContractDefinitions(connection, PARTICIPANT_CONTEXT_ID, TEMP_TABLE,
                List.of(DEFINITION_ID_1, DEFINITION_ID_2));
        inOrder.verify(queryManager).runAnalyze(connection, TEMP_TABLE);
        inOrder.verify(queryManager).findAssetWithMatchingContractDefinitions(connection, TEMP_TABLE, ASSET_ID);
        verifyNoMoreInteractions(dataSource, queryManager);
    }

    @Test
    void getById_shouldReturnNull_whenAssetIsNotFound() throws SQLException {
        when(contractDefinitionResolver.resolveFor(participantContext, agent)).thenReturn(authorizedDefinitions());
        when(queryManager.findAssetWithMatchingContractDefinitions(any(), anyString(), anyString())).thenReturn(Optional.empty());

        var dataset = resolver.getById(participantContext, agent, ASSET_ID, PROTOCOL);

        assertThat(dataset).isNull();

        var inOrder = inOrder(dataSource, queryManager);
        inOrder.verify(dataSource).getConnection();
        inOrder.verify(queryManager).createTemporaryContractDefinitionTable(connection);
        inOrder.verify(queryManager).copyAuthorizedContractDefinitions(connection, PARTICIPANT_CONTEXT_ID, TEMP_TABLE,
                List.of(DEFINITION_ID_1, DEFINITION_ID_2));
        inOrder.verify(queryManager).runAnalyze(connection, TEMP_TABLE);
        inOrder.verify(queryManager).findAssetWithMatchingContractDefinitions(connection, TEMP_TABLE, ASSET_ID);
        verifyNoMoreInteractions(dataSource, queryManager);
    }

    @Test
    void getById_shouldNotQueryDatabase_whenNoContractDefinitionIsAuthorized() {
        when(contractDefinitionResolver.resolveFor(participantContext, agent)).thenReturn(Map.of());

        var dataset = resolver.getById(participantContext, agent, ASSET_ID, PROTOCOL);

        assertThat(dataset).isNull();
        verifyNoInteractions(dataSource, queryManager);
    }

    private Map<String, ContractDefinition> authorizedDefinitions() {
        var definitions = new LinkedHashMap<String, ContractDefinition>();
        definitions.put(DEFINITION_ID_1, definition(DEFINITION_ID_1));
        definitions.put(DEFINITION_ID_2, definition(DEFINITION_ID_2));
        return definitions;
    }

    private ContractDefinition definition(String id) {
        return ContractDefinition.Builder.newInstance()
                .id(id)
                .participantContextId(PARTICIPANT_CONTEXT_ID)
                .accessPolicyId("access-policy")
                .contractPolicyId(CONTRACT_POLICY_ID)
                .build();
    }

    private Asset asset() {
        return Asset.Builder.newInstance()
                .id(ASSET_ID)
                .participantContextId(PARTICIPANT_CONTEXT_ID)
                .build();
    }

    private List<String> offeredDefinitionIds(Dataset dataset) {
        return dataset.getOffers().keySet().stream()
                .map(offerId -> ContractOfferId.parseId(offerId).getContent())
                .peek(offerId -> assertThat(offerId.assetIdPart()).isEqualTo(ASSET_ID))
                .map(ContractOfferId::definitionPart)
                .toList();
    }
}
