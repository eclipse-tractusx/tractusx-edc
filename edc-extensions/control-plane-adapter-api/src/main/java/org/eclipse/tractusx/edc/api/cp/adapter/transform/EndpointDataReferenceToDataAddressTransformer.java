/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.tractusx.edc.api.cp.adapter.transform;

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
