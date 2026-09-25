/********************************************************************************
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.controlplane.transform.edc.contractnegotiation.to;

import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.contract.spi.types.command.TerminateNegotiationCommand;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.TerminateNegotiation.TERMINATE_NEGOTIATION_REASON;

/// @deprecated This transformer is only used by the management API v3 and should be removed once v3 are removed.
/// This expects an `@id` field which is not used in API v4 and v5
@Deprecated(since = "0.17.0")
public class JsonObjectToTerminateNegotiationCommandTransformer extends AbstractJsonLdTransformer<JsonObject, TerminateNegotiationCommand> {

    public JsonObjectToTerminateNegotiationCommandTransformer() {
        super(JsonObject.class, TerminateNegotiationCommand.class);
    }

    @Override
    public @Nullable TerminateNegotiationCommand transform(@NotNull JsonObject input, @NotNull TransformerContext context) {
        var id = nodeId(input);
        var reason = transformString(input.get(TERMINATE_NEGOTIATION_REASON), context);

        return new TerminateNegotiationCommand(id, reason);
    }
}
