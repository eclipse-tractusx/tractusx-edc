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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * A request for asset data. The request may contain a transfer process ID or asset ID and must specify an endpoint for retrieving the data.
 */
@JsonDeserialize(builder = AssetRequest.Builder.class)
@JsonTypeName("tx:assetrequest")
public class AssetRequest {
    private String transferProcessId;
    private String assetId;

    private String providerId;
    private String endpointUrl;

    private String queryParams;

    private String pathSegments;

    private AssetRequest() {
    }

    public String getTransferProcessId() {
        return transferProcessId;
    }

    public String getAssetId() {
        return assetId;
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public String getProviderId() {
        return providerId;
    }

    public String getQueryParams() {
        return queryParams;
    }

    public String getPathSegments() {
        return pathSegments;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private final AssetRequest request;

        private Builder() {
            request = new AssetRequest();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder transferProcessId(String transferProcessId) {
            request.transferProcessId = transferProcessId;
            return this;
        }

        public Builder assetId(String assetId) {
            request.assetId = assetId;
            return this;
        }

        public Builder endpointUrl(String endpointUrl) {
            request.endpointUrl = endpointUrl;
            return this;
        }

        public Builder providerId(String providerId) {
            request.providerId = providerId;
            return this;
        }

        public Builder queryParams(String queryParams) {
            request.queryParams = queryParams;
            return this;
        }

        public Builder pathSegments(String pathSegments) {
            request.pathSegments = pathSegments;
            return this;
        }

        public AssetRequest build() {
            if (request.assetId == null && request.transferProcessId == null) {
                throw new NullPointerException("An assetId or endpointReferenceId must be set");
            }
            return request;
        }
    }
}
