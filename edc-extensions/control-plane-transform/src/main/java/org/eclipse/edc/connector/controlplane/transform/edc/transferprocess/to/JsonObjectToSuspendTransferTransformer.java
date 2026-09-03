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

package org.eclipse.edc.connector.controlplane.transform.edc.transferprocess.to;

import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.SuspendTransfer;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.connector.controlplane.transfer.spi.types.SuspendTransfer.SUSPEND_TRANSFER_REASON;

public class JsonObjectToSuspendTransferTransformer extends AbstractJsonLdTransformer<JsonObject, SuspendTransfer> {

    public JsonObjectToSuspendTransferTransformer() {
        super(JsonObject.class, SuspendTransfer.class);
    }

    @Override
    public @Nullable SuspendTransfer transform(@NotNull JsonObject input, @NotNull TransformerContext context) {
        var reason = transformString(input.get(SUSPEND_TRANSFER_REASON), context);
        return new SuspendTransfer(reason);
    }

}
