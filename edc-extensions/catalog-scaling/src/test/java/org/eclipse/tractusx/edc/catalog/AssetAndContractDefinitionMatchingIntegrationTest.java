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

import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.controlplane.store.sql.assetindex.SqlAssetIndex;
import org.eclipse.edc.connector.controlplane.store.sql.assetindex.schema.postgres.PostgresDialectStatements;
import org.eclipse.edc.connector.controlplane.store.sql.contractdefinition.SqlContractDefinitionStore;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.testfixtures.PostgresqlStoreSetupExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getResourceFileContentAsString;
import static org.eclipse.tractusx.edc.tests.testcontainer.PostgresContainerManager.getPostgresTestContainerName;

/**
 * Verifies that the Postgres functions correctly query assets and their matching contract definitions.
 */
@PostgresqlIntegrationTest
class AssetAndContractDefinitionMatchingIntegrationTest {

    private static final String PARTICIPANT_CONTEXT_ID = "participant";
    private static final String OTHER_PARTICIPANT_CONTEXT_ID = "other-participant";
    private static final String ASSET_ID = "asset";
    private static final String DEFINITION_ID = "definition";

    // for this test, all contract definitions are considered authorized, so no temporary table is used; contract definitions are read from original table
    private static final String DEFINITIONS_TABLE = "edc_contract_definitions";

    @RegisterExtension
    static final PostgresqlStoreSetupExtension EXTENSION = new PostgresqlStoreSetupExtension(getPostgresTestContainerName());

    private AssetIndex assetIndex;
    private ContractDefinitionStore contractDefinitionStore;
    private CatalogRequestQueryManager queryManager;

    @BeforeEach
    void setUp(QueryExecutor queryExecutor) {
        var mapper = new JacksonTypeManager().getMapper();
        assetIndex = new SqlAssetIndex(EXTENSION.getDataSourceRegistry(), EXTENSION.getDatasourceName(), EXTENSION.getTransactionContext(),
                mapper, new PostgresDialectStatements(), queryExecutor);
        contractDefinitionStore = new SqlContractDefinitionStore(EXTENSION.getDataSourceRegistry(), EXTENSION.getDatasourceName(), EXTENSION.getTransactionContext(),
                new org.eclipse.edc.connector.controlplane.store.sql.contractdefinition.schema.postgres.PostgresDialectStatements(),
                mapper, queryExecutor);

        queryManager = new CatalogRequestQueryManager(EXTENSION.getDataSourceRegistry(), EXTENSION.getDatasourceName(), EXTENSION.getTransactionContext(),
                mapper, queryExecutor, new WrappedAssetStatements(), 2);

        EXTENSION.runQuery(getResourceFileContentAsString("asset-index-schema.sql"));
        EXTENSION.runQuery(getResourceFileContentAsString("contract-definition-schema.sql"));
        EXTENSION.runQuery(getResourceFileContentAsString("sql/script.sql"));
    }

    @AfterEach
    void tearDown() {
        EXTENSION.runQuery("DROP TABLE edc_asset, edc_contract_definitions CASCADE");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource({"scalarCriteria", "collectionCriteria", "propertyLookup", "combinedSelectors"})
    void queryAssetWithMatchingDefinitionIds_verifyCriteriaEvaluatedCorrectly(MatchingCase testCase) throws SQLException {
        var asset = createAsset(ASSET_ID, PARTICIPANT_CONTEXT_ID, testCase.properties(), testCase.privateProperties());
        assertThat(assetIndex.create(asset)).isSucceeded();

        var definition = createContractDefinition(DEFINITION_ID, PARTICIPANT_CONTEXT_ID, testCase.selector());
        assertThat(contractDefinitionStore.save(definition)).isSucceeded();

        var expectedDefinitionIds = testCase.shouldMatch() ? List.of(DEFINITION_ID) : List.<String>of();
        verifySingleAssetQuery(ASSET_ID, expectedDefinitionIds);
        verifyBulkQuery(ASSET_ID, expectedDefinitionIds);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("multipleAssets")
    void queryAssetsWithMatchingDefinitionIds_verifyBulkQueryReturnsOnlyMatchingAsset(MultipleAssetsCase testCase) throws SQLException {
        var matchingAsset = createAsset(ASSET_ID, PARTICIPANT_CONTEXT_ID, testCase.matchingProperties(), testCase.matchingPrivateProperties());
        assertThat(assetIndex.create(matchingAsset)).isSucceeded();

        var nonMatchingAsset = createAsset("other-asset", PARTICIPANT_CONTEXT_ID, testCase.nonMatchingProperties(), testCase.nonMatchingPrivateProperties());
        assertThat(assetIndex.create(nonMatchingAsset)).isSucceeded();

        var definition = createContractDefinition(DEFINITION_ID, PARTICIPANT_CONTEXT_ID, testCase.selector());
        assertThat(contractDefinitionStore.save(definition)).isSucceeded();

        verifyBulkQuery(ASSET_ID, List.of(DEFINITION_ID));
    }

    @Test
    void queryAssetWithMatchingDefinitionIds_verifyNotMatchedWhenDifferentParticipantContextId() throws SQLException {
        assertThat(assetIndex.create(createAsset(ASSET_ID, OTHER_PARTICIPANT_CONTEXT_ID, Map.of("color", "red"),
                Map.of()))).isSucceeded();
        assertThat(contractDefinitionStore.save(createContractDefinition(DEFINITION_ID, PARTICIPANT_CONTEXT_ID,
                List.of(new Criterion("color", "=", "red"))))).isSucceeded();

        verifySingleAssetQuery(ASSET_ID, List.of());
        verifyBulkQuery(ASSET_ID, List.of());
    }

    /**
     * Verifies the bulk query (catalog request) returns only the asset with exactly the expected contract definition IDs, or nothing
     * if no definitions are expected.
     */
    private void verifyBulkQuery(String assetId, List<String> expectedDefinitionIds) throws SQLException {
        try (var connection = EXTENSION.getConnection();
            var assets = queryManager.queryAssetsWithMatchingContractDefinitions(connection, DEFINITIONS_TABLE, QuerySpec.max())) {
            var result = assets.toList();

            if (expectedDefinitionIds.isEmpty()) {
                assertThat(result).as("bulk query").isEmpty();
            } else {
                assertThat(result).as("bulk query").singleElement().satisfies(authorizedAsset -> {
                    assertThat(authorizedAsset.asset().getId()).isEqualTo(assetId);
                    assertThat(authorizedAsset.contractDefinitionIds()).containsExactlyInAnyOrderElementsOf(expectedDefinitionIds);
                });
            }
        }
    }

    /**
     * Verifies the single asset query (dataset request) returns the asset with exactly the expected contract definition IDs, or nothing
     * if no definitions are expected.
     */
    private void verifySingleAssetQuery(String assetId, List<String> expectedDefinitionIds) throws SQLException {
        try (var connection = EXTENSION.getConnection()) {
            var result = queryManager.findAssetWithMatchingContractDefinitions(connection, DEFINITIONS_TABLE, assetId);

            if (expectedDefinitionIds.isEmpty()) {
                assertThat(result).as("single asset query").isEmpty();
            } else {
                assertThat(result).as("single asset query").hasValueSatisfying(authorizedAsset -> {
                    assertThat(authorizedAsset.asset().getId()).isEqualTo(assetId);
                    assertThat(authorizedAsset.contractDefinitionIds()).containsExactlyInAnyOrderElementsOf(expectedDefinitionIds);
                });
            }
        }
    }

    /**
     * Represents a test case for matching an asset with the given properties and private properties
     * to a contract definition with the given assets selector criteria. Defines whether a successful
     * match is expected for this case or not.
     */
    private record MatchingCase(String name, Map<String, Object> properties, Map<String, Object> privateProperties, List<Criterion> selector, boolean shouldMatch) {
        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * Represents a test case for matching multiple assets with the given properties and private properties
     * to a contract definition with the given assets selector criteria. One asset should match the selector,
     * the other should not.
     */
    private record MultipleAssetsCase(String name, List<Criterion> selector,
                                      Map<String, Object> matchingProperties, Map<String, Object> matchingPrivateProperties,
                                      Map<String, Object> nonMatchingProperties, Map<String, Object> nonMatchingPrivateProperties) {
        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * Creates a case for testing whether a criterion's operator and right operand are evaluated
     * correctly against the asset property's value. A static property name is used, private
     * properties are not considered.
     */
    private static MatchingCase scalarCase(String name, Object value, String operator, Object operand, boolean matches) {
        return new MatchingCase(name, Collections.singletonMap("value", value), Map.of(),
                List.of(new Criterion("value", operator, operand)), matches);
    }

    /**
     * Creates a case for testing whether the correct property is resolved from an asset's properties
     * or private properties. The criterion always uses "=" as operator.
     */
    private static MatchingCase lookupCase(String name, Map<String, Object> properties, Map<String, Object> privateProperties,
                                           String leftOperand, Object rightOperand, boolean matches) {
        return new MatchingCase(name, properties, privateProperties, List.of(new Criterion(leftOperand, "=", rightOperand)), matches);
    }

    /**
     * Creates a case for testing 2 assets - one matching, one none-matching - against a contract
     * definition.
     */
    private static MultipleAssetsCase multipleAssetsCase(String name, Criterion criterion,
                                                         Map<String, Object> matchingProperties, Map<String, Object> nonMatchingProperties) {
        return new MultipleAssetsCase(name, List.of(criterion), matchingProperties, Map.of(), nonMatchingProperties, Map.of());
    }

    private Asset createAsset(String id, String participantContextId, Map<String, Object> properties, Map<String, Object> privateProperties) {
        return Asset.Builder.newInstance()
                .id(id)
                .participantContextId(participantContextId)
                .properties(properties)
                .privateProperties(privateProperties)
                .build();
    }

    private ContractDefinition createContractDefinition(String id, String participantContextId, List<Criterion> assetsSelector) {
        return ContractDefinition.Builder.newInstance()
                .id(id)
                .participantContextId(participantContextId)
                .accessPolicyId("accessPolicyId")
                .contractPolicyId("contractPolicyId")
                .assetsSelector(assetsSelector)
                .build();
    }

    private static Stream<MatchingCase> scalarCriteria() {
        return Stream.of(
                scalarCase("equal strings", "red", "=", "red", true),
                scalarCase("unequal strings", "blue", "=", "red", false),
                scalarCase("string equality is case sensitive", "RED", "=", "red", false),
                scalarCase("not equal", "blue", "!=", "red", true),
                scalarCase("not equal rejects equal value", "red", "!=", "red", false),
                scalarCase("numeric equality", 5, "=", 5, true),
                scalarCase("numeric equality across types", 5, "=", 5.0, true),
                scalarCase("numbers are not equal to strings", 5, "=", "5", false),
                scalarCase("less than", 5, "<", 6, true),
                scalarCase("less than excludes boundary", 5, "<", 5, false),
                scalarCase("less than or equal includes boundary", 5, "<=", 5, true),
                scalarCase("less than or equal rejects larger", 6, "<=", 5, false),
                scalarCase("greater than", 6, ">", 5, true),
                scalarCase("greater than excludes boundary", 5, ">", 5, false),
                scalarCase("greater than or equal includes boundary", 5, ">=", 5, true),
                scalarCase("greater than or equal rejects smaller", 4, ">=", 5, false),
                scalarCase("string ordering", "apple", "<", "banana", true),
                scalarCase("string ordering is case sensitive", "a", "<", "Z", false),
                scalarCase("mixed types are not comparable", 5, "<", "6", false),
                scalarCase("booleans are not ordered", false, "<", true, false),
                scalarCase("boolean equality", true, "=", true, true),
                scalarCase("boolean inequality", false, "=", true, false),
                scalarCase("boolean parses string operand", true, "=", "TrUe", true),
                scalarCase("boolean parses non-true string as false", false, "=", "anything", true),
                scalarCase("boolean parses numeric operand as false", false, "=", 1, true),
                scalarCase("boolean does not equal null", false, "=", null, false),
                scalarCase("like wildcard", "VehicleData", "like", "Vehicle%", true),
                scalarCase("like single character", "VehicleData", "like", "VehicleDat_", true),
                scalarCase("like must match whole value", "VehicleData", "like", "Vehicle", false),
                scalarCase("like is case sensitive", "VehicleData", "like", "vehicle%", false),
                scalarCase("ilike ignores case", "VehicleData", "ilike", "vehicle%", true),
                scalarCase("ilike rejects different value", "VehicleData", "ilike", "parts%", false),
                scalarCase("like treats regex syntax literally", "a.b", "like", "a.b", true),
                scalarCase("like requires string operand", "5", "like", 5, false),
                scalarCase("like converts scalar property to string", 123, "like", "12%", true),
                scalarCase("operators are case insensitive", "red", "IN", List.of("red"), true),
                scalarCase("null property never matches", null, "=", null, false),
                scalarCase("null property never matches inequality", null, "!=", "red", false),
                scalarCase("null property never matches not in", null, "not in", List.of("red"), false)
        );
    }

    private static Stream<MatchingCase> collectionCriteria() {
        return Stream.of(
                scalarCase("in matches scalar", "red", "in", List.of("red", "blue"), true),
                scalarCase("in rejects absent scalar", "green", "in", List.of("red", "blue"), false),
                scalarCase("in empty list matches nothing", "red", "in", List.of(), false),
                scalarCase("not in matches absent scalar", "green", "not in", List.of("red", "blue"), true),
                scalarCase("not in rejects present scalar", "red", "not in", List.of("red", "blue"), false),
                scalarCase("not in empty list", "red", "not in", List.of(), true),
                scalarCase("equality checks list membership", List.of("red", "blue"), "=", "red", true),
                scalarCase("equality rejects absent list member", List.of("blue"), "=", "red", false),
                scalarCase("equality rejects empty list", List.of(), "=", "red", false),
                scalarCase("list equality does not flatten nested lists", List.of(List.of("red")), "=", "red", false),
                scalarCase("list equality matches boolean member", List.of(true, false), "=", true, true),
                scalarCase("contains list member", List.of("red", "blue"), "contains", "red", true),
                scalarCase("contains rejects absent list member", List.of("blue"), "contains", "red", false),
                scalarCase("contains does not accept a string property", "red", "contains", "red", false),
                scalarCase("in does not unwrap list properties", List.of("red"), "in", List.of("red"), false),
                scalarCase("in compares whole lists", List.of("red"), "in", List.of(List.of("red")), true),
                scalarCase("not in compares whole lists", List.of("red"), "not in", List.of("red"), true),
                scalarCase("in compares whole objects", Map.of("color", "red"), "in", List.of(Map.of("color", "red")), true),
                scalarCase("in requires exact object equality", Map.of("color", "red", "size", 5), "in",
                        List.of(Map.of("color", "red")), false),
                scalarCase("object equality is not supported", Map.of("color", "red"), "=", Map.of("color", "red"), false),
                scalarCase("list equality can select object member", List.of(Map.of("color", "red")), "=", Map.of("color", "red"), true),
                scalarCase("list contains null", Arrays.asList("red", null), "contains", null, true)
        );
    }

    private static Stream<MatchingCase> propertyLookup() {
        return Stream.of(
                lookupCase("private property", Map.of(), Map.of("color", "red"), "color", "red", true),
                lookupCase("private nested property", Map.of(), Map.of("data", Map.of("color", "red")), "data.color", "red", true),
                lookupCase("nested property", Map.of("data", Map.of("color", "red")), Map.of(), "data.color", "red", true),
                lookupCase("whole dotted key", Map.of("data.color", "red"), Map.of(), "data.color", "red", true),
                lookupCase("quoted dotted key", Map.of("data.color", "red"), Map.of(), "'data.color'", "red", true),
                lookupCase("partially quoted path", Map.of("data", Map.of("color.name", "red")), Map.of(), "data.'color.name'", "red", true),
                lookupCase("path takes precedence over whole key (non-match case)", Map.of("data", Map.of("color", "blue"), "data.color", "red"),
                        Map.of(), "data.color", "red", false),
                lookupCase("path takes precedence over whole key (match case)", Map.of("data", Map.of("color", "red"), "data.color", "blue"),
                        Map.of(), "data.color", "red", true),
                lookupCase("public whole key precedes private path (non-match case)", Map.of("data.color", "blue"),
                        Map.of("data", Map.of("color", "red")), "data.color", "red", false),
                lookupCase("public whole key precedes private path (match case)", Map.of("data.color", "red"),
                        Map.of("data", Map.of("color", "blue")), "data.color", "red", true),
                lookupCase("public path precedes private whole key", Map.of("data", Map.of("color", "blue")),
                        Map.of("data.color", "red"), "data.color", "red", false),
                lookupCase("private path precedes private whole key", Map.of(),
                        Map.of("data", Map.of("color", "blue"), "data.color", "red"), "data.color", "red", false),
                lookupCase("private dotted key", Map.of(), Map.of("data.color", "red"), "data.color", "red", true),
                lookupCase("null path falls back to whole key",
                        Map.of("data", Collections.singletonMap("color", null), "data.color", "red"),
                        Map.of(), "data.color", "red", true),
                lookupCase("missing property", Map.of(), Map.of(), "color", "red", false),
                lookupCase("array index", Map.of("colors", List.of("red", "blue")), Map.of(), "colors[1]", "blue", true),
                lookupCase("array index selects only that element", Map.of("colors", List.of("red", "blue")),
                        Map.of(), "colors[1]", "red", false),
                lookupCase("path through array does not unwrap maps",
                        Map.of("data", List.of(Map.of("color", "red"))), Map.of(), "data.color", "red", false),
                lookupCase("nested list membership", Map.of("data", Map.of("colors", List.of("red", "blue"))),
                        Map.of(), "data.colors", "red", true),
                lookupCase("URI property", Map.of("https://example.org/color", "red"),
                        Map.of(), "https://example.org/color", "red", true),
                lookupCase("double quote in key", Map.of("col\"or", "red"), Map.of(), "col\"or", "red", true),
                lookupCase("asset id property", Map.of(), Map.of(), Asset.PROPERTY_ID, ASSET_ID, true)
        );
    }

    private static Stream<MatchingCase> combinedSelectors() {
        return Stream.of(
                new MatchingCase("empty selector selects all", Map.of(), Map.of(), List.of(), true),
                new MatchingCase("all equalities must match", Map.of("color", "red", "size", 5), Map.of(),
                        List.of(new Criterion("color", "=", "red"), new Criterion("size", "=", 5)), true),
                new MatchingCase("one failed equality rejects asset", Map.of("color", "red", "size", 5), Map.of(),
                        List.of(new Criterion("color", "=", "red"), new Criterion("size", "=", 6)), false),
                new MatchingCase("fast path and remaining criteria match", Map.of("color", "red", "size", 5), Map.of(),
                        List.of(new Criterion("color", "in", List.of("red", "blue")), new Criterion("size", ">", 4)), true),
                new MatchingCase("remaining criterion rejects fast path candidate", Map.of("color", "red", "size", 5), Map.of(),
                        List.of(new Criterion("color", "=", "red"), new Criterion("size", ">", 5)), false),
                new MatchingCase("only remaining criteria", Map.of("color", "red", "size", 5), Map.of(),
                        List.of(new Criterion("color", "like", "r%"), new Criterion("size", ">=", 5)), true),
                new MatchingCase("contradictory criteria", Map.of("color", "red"), Map.of(),
                        List.of(new Criterion("color", "=", "red"), new Criterion("color", "=", "blue")), false),
                new MatchingCase("duplicate keys retain every criterion", Map.of("colors", List.of("red", "blue")), Map.of(),
                        List.of(new Criterion("colors", "=", "red"), new Criterion("colors", "=", "blue")), true),
                new MatchingCase("missing property fails negative operator", Map.of("color", "red"), Map.of(),
                        List.of(new Criterion("color", "=", "red"), new Criterion("missing", "not in", List.of("blue"))), false)
        );
    }

    private static Stream<MultipleAssetsCase> multipleAssets() {
        return Stream.of(
                multipleAssetsCase("equality", new Criterion("color", "=", "red"),
                        Map.of("color", "red"), Map.of("color", "blue")),
                multipleAssetsCase("in", new Criterion("color", "in", List.of("red", "green")),
                        Map.of("color", "red"), Map.of("color", "blue")),
                multipleAssetsCase("not in", new Criterion("color", "not in", List.of("blue")),
                        Map.of("color", "red"), Map.of("color", "blue")),
                multipleAssetsCase("range", new Criterion("size", ">=", 5),
                        Map.of("size", 5), Map.of("size", 4)),
                multipleAssetsCase("like", new Criterion("name", "like", "Vehicle%"),
                        Map.of("name", "VehicleData"), Map.of("name", "PartData")),
                multipleAssetsCase("list membership", new Criterion("colors", "=", "red"),
                        Map.of("colors", List.of("red", "blue")), Map.of("colors", List.of("blue"))),
                multipleAssetsCase("nested property", new Criterion("data.color", "=", "red"),
                        Map.of("data", Map.of("color", "red")), Map.of("data", Map.of("color", "blue"))),
                multipleAssetsCase("missing property", new Criterion("color", "=", "red"),
                        Map.of("color", "red"), Map.of("size", 5)),
                new MultipleAssetsCase("private property", List.of(new Criterion("color", "=", "red")),
                        Map.of(), Map.of("color", "red"), Map.of(), Map.of("color", "blue")),
                new MultipleAssetsCase("fast path and remaining criteria",
                        List.of(new Criterion("color", "=", "red"), new Criterion("size", ">", 4)),
                        Map.of("color", "red", "size", 5), Map.of(), Map.of("color", "red", "size", 4), Map.of())
        );
    }
}
