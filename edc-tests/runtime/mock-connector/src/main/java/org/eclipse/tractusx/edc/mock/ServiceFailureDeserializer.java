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
import java.util.Arrays;

/**
 * Custom deserializer for {@link ServiceFailure}. We need this because there is no default
 * CTor, and the public constructor with args is not annotated with {@link com.fasterxml.jackson.annotation.JsonProperty}.
 */
@Deprecated(since = "0.11.0")
public class ServiceFailureDeserializer extends StdDeserializer<ServiceFailure> {
    public static final String REASON_FIELD = "reason";
    public static final String MESSAGES_FIELD = "messages";

    protected ServiceFailureDeserializer(Class<?> vc) {
        super(vc);
    }

    public ServiceFailureDeserializer() {
        this(null);
    }

    @Override
    public ServiceFailure deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);
        var reason = ServiceFailure.Reason.valueOf(node.get(REASON_FIELD).asText());
        var msgs = Arrays.asList(ctxt.readTreeAsValue(node.get(MESSAGES_FIELD), String[].class));

        return new ServiceFailure(msgs, reason);
    }
}
