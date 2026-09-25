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

package org.eclipse.edc.connector.controlplane.transform.edc.participantcontext.config.to;

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.participantcontext.spi.config.model.ParticipantContextConfiguration;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;

import static org.eclipse.edc.participantcontext.spi.config.model.ParticipantContextConfiguration.PARTICIPANT_CONTEXT_CONFIG_ENTRIES_IRI;
import static org.eclipse.edc.participantcontext.spi.config.model.ParticipantContextConfiguration.PARTICIPANT_CONTEXT_CONFIG_PRIVATE_ENTRIES_IRI;

public class JsonObjectToParticipantContextConfigurationTransformer extends AbstractJsonLdTransformer<JsonObject, ParticipantContextConfiguration> {
    public JsonObjectToParticipantContextConfigurationTransformer() {
        super(JsonObject.class, ParticipantContextConfiguration.class);
    }

    @Override
    public @Nullable ParticipantContextConfiguration transform(@NotNull JsonObject jsonObject, @NotNull TransformerContext context) {
        var participantContext = ParticipantContextConfiguration.Builder.newInstance();

        var properties = jsonObject.get(PARTICIPANT_CONTEXT_CONFIG_ENTRIES_IRI);
        if (readProperties(context, properties, participantContext::entry)) return null;

        var privateProperties = jsonObject.get(PARTICIPANT_CONTEXT_CONFIG_PRIVATE_ENTRIES_IRI);
        if (readProperties(context, privateProperties, participantContext::privateEntry)) return null;

        return participantContext.build();
    }

    private boolean readProperties(@NotNull TransformerContext context, JsonValue properties, BiConsumer<String, String> action) {
        if (properties != null) {
            var jsonValue = nodeJsonValue(properties);
            if (jsonValue instanceof JsonObject json) {
                visitProperties(json, (key, value) -> action.accept(key, transformString(value, context)));
            } else {
                context.reportProblem("Expected properties to be a JsonObject");
                return true;
            }
        }
        return false;
    }
}
