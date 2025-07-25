/*
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft
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

package org.eclipse.tractusx.edc.mock;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.eclipse.edc.spi.result.ServiceFailure;

import java.io.IOException;
import java.util.Optional;

/**
 * Custom deserializer for a {@link RecordedRequest} object, to be able to instantiate the input and output objects according
 * to their class description.
 */
@Deprecated(since = "0.11.0")
class RecordedResponseDeserializer extends StdDeserializer<RecordedRequest<?, ?>> {

    public static final String INPUT_OBJECT = "input";
    public static final String OUTPUT_OBJECT = "output";
    public static final String CLASS_FIELD = "class";
    public static final String DATA_FIELD = "data";
    public static final String INPUT_MATCH_TYPE_FIELD = "match_type";
    public static final String NAME_FIELD = "name";
    public static final String DESCRIPTION_FIELD = "description";
    private static final String FAILURE_OBJECT = "failure";

    RecordedResponseDeserializer() {
        this(null);
    }

    protected RecordedResponseDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public RecordedRequest<?, ?> deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);
        var input = node.get(INPUT_OBJECT);
        var output = node.get(OUTPUT_OBJECT);
        try {
            var inputClass = Class.forName(input.get(CLASS_FIELD).asText());
            var outputClass = Class.forName(output.get(CLASS_FIELD).asText());

            var inputObj = ctxt.readTreeAsValue(input.get(DATA_FIELD), inputClass);
            var matchType = Optional.ofNullable(input.get("matchType")).map(JsonNode::asText).map(MatchType::valueOf).orElse(MatchType.CLASS);
            var outputObj = ctxt.readTreeAsValue(output.get(DATA_FIELD), outputClass);

            return new RecordedRequest.Builder(inputObj, outputObj)
                    .inputMatchType(matchType)
                    .failure(ctxt.readTreeAsValue(node.get(FAILURE_OBJECT), ServiceFailure.class))
                    .name(Optional.ofNullable(node.get(NAME_FIELD)).map(JsonNode::asText).orElse(null))
                    .description(Optional.ofNullable(node.get(DESCRIPTION_FIELD)).map(JsonNode::asText).orElse(null))
                    .build();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
