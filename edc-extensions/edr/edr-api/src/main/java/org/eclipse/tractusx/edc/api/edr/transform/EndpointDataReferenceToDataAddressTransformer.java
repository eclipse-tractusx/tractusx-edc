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

package org.eclipse.tractusx.edc.api.edr.transform;

import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.eclipse.edc.transform.spi.TypeTransformer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.spi.types.domain.edr.EndpointDataReference.AUTH_CODE;
import static org.eclipse.edc.spi.types.domain.edr.EndpointDataReference.AUTH_KEY;
import static org.eclipse.edc.spi.types.domain.edr.EndpointDataReference.EDR_SIMPLE_TYPE;
import static org.eclipse.edc.spi.types.domain.edr.EndpointDataReference.ENDPOINT;
import static org.eclipse.edc.spi.types.domain.edr.EndpointDataReference.ID;

public class EndpointDataReferenceToDataAddressTransformer implements TypeTransformer<EndpointDataReference, DataAddress> {
    @Override
    public Class<EndpointDataReference> getInputType() {
        return EndpointDataReference.class;
    }

    @Override
    public Class<DataAddress> getOutputType() {
        return DataAddress.class;
    }

    @Override
    public @Nullable DataAddress transform(@NotNull EndpointDataReference edr, @NotNull TransformerContext context) {
        return DataAddress.Builder.newInstance()
                .type(EDR_SIMPLE_TYPE)
                .property(ID, edr.getId())
                .property(AUTH_CODE, edr.getAuthCode())
                .property(AUTH_KEY, edr.getAuthKey())
                .property(ENDPOINT, edr.getEndpoint())
                .properties(edr.getProperties())
                .build();
    }
}
