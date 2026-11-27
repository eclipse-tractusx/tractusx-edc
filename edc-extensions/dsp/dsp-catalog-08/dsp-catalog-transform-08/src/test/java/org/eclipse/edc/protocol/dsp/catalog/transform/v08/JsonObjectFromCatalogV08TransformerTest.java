/********************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
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
 * Assisted-By: Claude Code (claude-opus-5-5)
 ********************************************************************************/

package org.eclipse.edc.protocol.dsp.catalog.transform.v08;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.catalog.spi.Catalog;
import org.eclipse.edc.connector.controlplane.catalog.spi.DataService;
import org.eclipse.edc.connector.controlplane.catalog.spi.Dataset;
import org.eclipse.edc.participant.spi.ParticipantIdMapper;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCAT_CATALOG_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCAT_CATALOG_TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCAT_DATASET_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCAT_DATA_SERVICE_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCAT_DISTRIBUTION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DSPACE_PROPERTY_PARTICIPANT_ID_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.Dsp08Constants.DSP_NAMESPACE_V_08;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JsonObjectFromCatalogV08TransformerTest {

    private final TransformerContext context = mock();
    private final TypeManager typeManager = mock();
    private final ParticipantIdMapper participantIdMapper = mock();
    private JsonObjectFromCatalogV08Transformer transformer;

    @BeforeEach
    void setUp() {
        when(typeManager.getMapper("test")).thenReturn(new ObjectMapper());
        when(participantIdMapper.toIri(any())).thenAnswer(i -> i.getArgument(0));
        transformer = new JsonObjectFromCatalogV08Transformer(Json.createBuilderFactory(Map.of()), typeManager, "test",
                participantIdMapper, DSP_NAMESPACE_V_08);
    }

    @Test
    void transform_shouldEmitEmptyArrays_whenCatalogIsEmpty() {
        var catalog = Catalog.Builder.newInstance().id("catalog-id").participantId("BPNL000000000001").build();

        var result = transformer.transform(catalog, context);

        assertThat(result).isNotNull();
        assertThat(result.getString(ID)).isEqualTo("catalog-id");
        assertThat(result.getString(TYPE)).isEqualTo(DCAT_CATALOG_TYPE);
        assertThat(result.getJsonArray(DCAT_DATASET_ATTRIBUTE)).isEmpty();
        assertThat(result.getJsonArray(DCAT_CATALOG_ATTRIBUTE)).isEmpty();
        assertThat(result.getJsonArray(DCAT_DISTRIBUTION_ATTRIBUTE)).isEmpty();
        assertThat(result.getJsonArray(DCAT_DATA_SERVICE_ATTRIBUTE)).isEmpty();
        assertThat(result.getJsonObject(DSP_NAMESPACE_V_08.toIri(DSPACE_PROPERTY_PARTICIPANT_ID_TERM)).getString(ID))
                .isEqualTo("BPNL000000000001");
    }

    @Test
    void transform_shouldSeparateDatasetsFromSubCatalogs() {
        var dataset = Dataset.Builder.newInstance().id("dataset").build();
        var subCatalog = Catalog.Builder.newInstance().id("sub-catalog").build();
        var dataService = DataService.Builder.newInstance().id("service").build();
        var catalog = Catalog.Builder.newInstance().id("catalog-id").dataset(dataset).dataset(subCatalog)
                .dataService(dataService).build();
        var datasetJson = Json.createObjectBuilder().add(ID, "dataset").build();
        var subCatalogJson = Json.createObjectBuilder().add(ID, "sub-catalog").build();
        var dataServiceJson = Json.createObjectBuilder().add(ID, "service").build();
        when(context.transform(dataset, JsonObject.class)).thenReturn(datasetJson);
        when(context.transform(eq(subCatalog), eq(JsonObject.class))).thenReturn(subCatalogJson);
        when(context.transform(dataService, JsonObject.class)).thenReturn(dataServiceJson);

        var result = transformer.transform(catalog, context);

        assertThat(result).isNotNull();
        assertThat(result.getJsonArray(DCAT_DATASET_ATTRIBUTE)).containsExactly(datasetJson);
        assertThat(result.getJsonArray(DCAT_CATALOG_ATTRIBUTE)).containsExactly(subCatalogJson);
        assertThat(result.getJsonArray(DCAT_DATA_SERVICE_ATTRIBUTE)).containsExactly(dataServiceJson);
        assertThat(result.getJsonArray(DCAT_DISTRIBUTION_ATTRIBUTE)).isEmpty();
    }
}
