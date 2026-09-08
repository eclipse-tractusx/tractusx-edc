/********************************************************************************
 * Copyright (c) 2025 Metaform Systems, Inc.
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

package org.eclipse.edc.connector.controlplane.transform.edc.participantcontext.config.from;

import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.participantcontext.spi.config.model.ParticipantContextConfiguration;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.JSON;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.eclipse.edc.participantcontext.spi.config.model.ParticipantContextConfiguration.PARTICIPANT_CONTEXT_CONFIG_ENTRIES_IRI;
import static org.eclipse.edc.participantcontext.spi.config.model.ParticipantContextConfiguration.PARTICIPANT_CONTEXT_CONFIG_PRIVATE_ENTRIES_IRI;
import static org.eclipse.edc.participantcontext.spi.config.model.ParticipantContextConfiguration.PARTICIPANT_CONTEXT_CONFIG_TYPE_IRI;

public class JsonObjectFromParticipantContextConfigurationTransformer extends AbstractJsonLdTransformer<ParticipantContextConfiguration, JsonObject> {

    private final JsonBuilderFactory jsonFactory;

    public JsonObjectFromParticipantContextConfigurationTransformer(JsonBuilderFactory jsonFactory) {
        super(ParticipantContextConfiguration.class, JsonObject.class);
        this.jsonFactory = jsonFactory;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull ParticipantContextConfiguration config, @NotNull TransformerContext context) {
        return jsonFactory.createObjectBuilder()
                .add(TYPE, PARTICIPANT_CONTEXT_CONFIG_TYPE_IRI)
                .add(PARTICIPANT_CONTEXT_CONFIG_ENTRIES_IRI, createProperties(config.getEntries()))
                .add(PARTICIPANT_CONTEXT_CONFIG_PRIVATE_ENTRIES_IRI, createProperties(config.getPrivateEntries()))
                .build();
    }

    private JsonObject createProperties(Map<String, String> config) {
        var entries = jsonFactory.createObjectBuilder();
        config.forEach(entries::add);

        return jsonFactory.createObjectBuilder()
                .add(VALUE, entries)
                .add(TYPE, JSON)
                .build();
    }
}
