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

import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.spi.types.domain.edr.EndpointDataReference.AUTH_CODE;
import static org.eclipse.edc.spi.types.domain.edr.EndpointDataReference.AUTH_KEY;
import static org.eclipse.edc.spi.types.domain.edr.EndpointDataReference.Builder;
import static org.eclipse.edc.spi.types.domain.edr.EndpointDataReference.EDR_SIMPLE_TYPE;
import static org.eclipse.edc.spi.types.domain.edr.EndpointDataReference.ENDPOINT;
import static org.eclipse.edc.spi.types.domain.edr.EndpointDataReference.ID;
import static org.mockito.Mockito.mock;

public class EndpointDataReferenceToDataAddressTransformerTest {

    private final TransformerContext context = mock(TransformerContext.class);
    private EndpointDataReferenceToDataAddressTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new EndpointDataReferenceToDataAddressTransformer();
    }

    @Test
    void transform() {

        var dto = Builder.newInstance()
                .id("dataRequestId")
                .authCode("authCode")
                .authKey("authKey")
                .endpoint("http://endpoint")
                .build();

        var dataAddress = transformer.transform(dto, context);

        assertThat(dataAddress).isNotNull();
        assertThat(dataAddress.getType()).isEqualTo(EDR_SIMPLE_TYPE);
        assertThat(dataAddress.getProperty(ID)).isEqualTo(dto.getId());
        assertThat(dataAddress.getProperty(ENDPOINT)).isEqualTo(dto.getEndpoint());
        assertThat(dataAddress.getProperty(AUTH_KEY)).isEqualTo(dto.getAuthKey());
        assertThat(dataAddress.getProperty(AUTH_CODE)).isEqualTo(dto.getAuthCode());

    }
}
