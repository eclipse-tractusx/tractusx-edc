/********************************************************************************
 * Copyright (c) 2026 Think-it GmbH
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

import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.catalog.spi.Catalog;
import org.eclipse.edc.connector.controlplane.catalog.spi.Dataset;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.jsonld.spi.transformer.AbstractNamespaceAwareJsonLdTransformer;
import org.eclipse.edc.participant.spi.ParticipantIdMapper;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.stream.Collectors;

import static jakarta.json.stream.JsonCollectors.toJsonArray;
import static java.util.Optional.ofNullable;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCAT_CATALOG_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCAT_CATALOG_TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCAT_DATASET_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCAT_DATA_SERVICE_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCAT_DISTRIBUTION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DSPACE_PROPERTY_PARTICIPANT_ID_TERM;

/**
 * Converts from a {@link Catalog} to a DCAT catalog as a {@link JsonObject} in JSON-LD expanded form.
 * <p>
 * DSP 0.8 backport of the upstream {@code JsonObjectFromCatalogV08Transformer} (EDC 0.17.0), which was removed in EDC 0.18.
 * Unlike the upstream {@code JsonObjectFromCatalogV2025Transformer}, empty {@code dcat:dataset}, {@code dcat:catalog},
 * {@code dcat:distribution} and {@code dcat:service} arrays are always emitted to keep the DSP 0.8 wire format stable.
 */
public class JsonObjectFromCatalogV08Transformer extends AbstractNamespaceAwareJsonLdTransformer<Catalog, JsonObject> {
    private final JsonBuilderFactory jsonFactory;
    private final TypeManager typeManager;
    private final String typeContext;
    private final ParticipantIdMapper participantIdMapper;

    public JsonObjectFromCatalogV08Transformer(JsonBuilderFactory jsonFactory, TypeManager typeManager, String typeContext, ParticipantIdMapper participantIdMapper, JsonLdNamespace namespace) {
        super(Catalog.class, JsonObject.class, namespace);
        this.jsonFactory = jsonFactory;
        this.typeManager = typeManager;
        this.typeContext = typeContext;
        this.participantIdMapper = participantIdMapper;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull Catalog catalog, @NotNull TransformerContext context) {
        var partitions = catalog.getDatasets().stream().collect(Collectors.groupingBy(Dataset::getClass));

        var datasets = ofNullable(partitions.get(Dataset.class)).orElseGet(ArrayList::new)
                .stream()
                .map(offer -> context.transform(offer, JsonObject.class))
                .collect(toJsonArray());

        var subCatalogs = ofNullable(partitions.get(Catalog.class)).orElseGet(ArrayList::new)
                .stream()
                .map(offer -> context.transform(offer, JsonObject.class))
                .collect(toJsonArray());

        var dataServices = catalog.getDataServices().stream()
                .map(service -> context.transform(service, JsonObject.class))
                .collect(toJsonArray());

        var distributions = catalog.getDistributions().stream()
                .map(distro -> context.transform(distro, JsonObject.class))
                .collect(toJsonArray());

        var objectBuilder = jsonFactory.createObjectBuilder()
                .add(ID, catalog.getId())
                .add(TYPE, DCAT_CATALOG_TYPE)
                .add(DCAT_DATASET_ATTRIBUTE, datasets)
                .add(DCAT_CATALOG_ATTRIBUTE, subCatalogs)
                .add(DCAT_DISTRIBUTION_ATTRIBUTE, distributions)
                .add(DCAT_DATA_SERVICE_ATTRIBUTE, dataServices);

        ofNullable(catalog.getParticipantId()).ifPresent(pid -> objectBuilder.add(forNamespace(DSPACE_PROPERTY_PARTICIPANT_ID_TERM), createId(jsonFactory, participantIdMapper.toIri(pid))));

        transformProperties(catalog.getProperties(), objectBuilder, typeManager.getMapper(typeContext), context);

        return objectBuilder.build();
    }

}
