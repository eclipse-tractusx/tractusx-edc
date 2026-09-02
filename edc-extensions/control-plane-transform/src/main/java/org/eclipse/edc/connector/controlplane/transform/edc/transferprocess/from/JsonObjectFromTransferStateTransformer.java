/********************************************************************************
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.controlplane.transform.edc.transferprocess.from;

import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferState;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferState.TRANSFER_STATE_STATE;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferState.TRANSFER_STATE_TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;

public class JsonObjectFromTransferStateTransformer extends AbstractJsonLdTransformer<TransferState, JsonObject> {

    private final JsonBuilderFactory builderFactory;

    public JsonObjectFromTransferStateTransformer(JsonBuilderFactory builderFactory) {
        super(TransferState.class, JsonObject.class);
        this.builderFactory = builderFactory;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull TransferState input, @NotNull TransformerContext context) {
        var builder = builderFactory.createObjectBuilder();
        builder.add(TYPE, TRANSFER_STATE_TYPE);
        builder.add(TRANSFER_STATE_STATE, input.state());
        return builder.build();
    }

}
