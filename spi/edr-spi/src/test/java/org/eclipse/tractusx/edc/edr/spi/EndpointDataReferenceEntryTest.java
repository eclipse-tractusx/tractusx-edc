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

package org.eclipse.tractusx.edc.edr.spi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntry;
import org.junit.jupiter.api.Test;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;

class EndpointDataReferenceEntryTest {

    @Test
    void verify_serializeDeserialize() throws JsonProcessingException {
        var mapper = new ObjectMapper();

        var entry = EndpointDataReferenceEntry.Builder.newInstance()
                .assetId(randomUUID().toString())
                .agreementId(randomUUID().toString())
                .transferProcessId(randomUUID().toString())
                .build();

        var serialized = mapper.writeValueAsString(entry);
        var deserialized = mapper.readValue(serialized, EndpointDataReferenceEntry.class);

        assertThat(deserialized.getTransferProcessId()).isNotEmpty();
        assertThat(deserialized.getAssetId()).isNotEmpty();
        assertThat(deserialized.getAgreementId()).isNotEmpty();
    }
}
