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

package org.eclipse.tractusx.edc.dataplane.proxy.consumer.api.asset.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AssetRequestTest {

    @Test
    void verify_SerializeDeserialize() throws JsonProcessingException {
        var mapper = new ObjectMapper();

        var request = AssetRequest.Builder.newInstance()
                .assetId("asset1")
                .endpointUrl("https://test.com")
                .providerId("providerId")
                .transferProcessId("tp1")
                .queryParams("params")
                .pathSegments("path")
                .build();

        var serialized = mapper.writeValueAsString(request);

        var deserialized = mapper.readValue(serialized, AssetRequest.class);

        assertThat(deserialized.getAssetId()).isEqualTo(request.getAssetId());
        assertThat(deserialized.getTransferProcessId()).isEqualTo(request.getTransferProcessId());
        assertThat(deserialized.getEndpointUrl()).isEqualTo(request.getEndpointUrl());
        assertThat(deserialized.getProviderId()).isEqualTo(request.getProviderId());
        assertThat(deserialized.getPathSegments()).isEqualTo(request.getPathSegments());
        assertThat(deserialized.getQueryParams()).isEqualTo(request.getQueryParams());

    }

    @Test
    void verify_NullArguments() {
        assertThatThrownBy(() -> AssetRequest.Builder.newInstance().build()).isInstanceOf(NullPointerException.class);
    }

    @Test
    void verify_AssetIdOrTransferProcessId() {
        AssetRequest.Builder.newInstance().assetId("asset1").endpointUrl("https://test.com").build();
        AssetRequest.Builder.newInstance().transferProcessId("tp1").endpointUrl("https://test.com").build();
    }
}
