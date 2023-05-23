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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;

import static java.util.Objects.requireNonNull;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.TX_NAMESPACE;

/**
 * An entry in the cache for an {@link EndpointDataReference}.
 */
@JsonDeserialize(builder = EndpointDataReferenceEntry.Builder.class)
public class EndpointDataReferenceEntry {

    public static final String SIMPLE_TYPE = "EndpointDataReferenceEntry";

    public static final String EDR_ENTRY_TYPE = TX_NAMESPACE + SIMPLE_TYPE;
    public static final String EDR_ENTRY_ASSET_ID = EDC_NAMESPACE + "assetId";
    public static final String EDR_ENTRY_AGREEMENT_ID = EDC_NAMESPACE + "agreementId";
    public static final String EDR_ENTRY_TRANSFER_PROCESS_ID = EDC_NAMESPACE + "transferProcessId";

    private String assetId;
    private String agreementId;
    private String transferProcessId;

    private EndpointDataReferenceEntry() {
    }

    public String getAssetId() {
        return assetId;
    }

    public String getAgreementId() {
        return agreementId;
    }

    public String getTransferProcessId() {
        return transferProcessId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        var that = (EndpointDataReferenceEntry) o;

        return transferProcessId.equals(that.transferProcessId);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private final EndpointDataReferenceEntry entry;

        private Builder() {
            entry = new EndpointDataReferenceEntry();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder assetId(String assetId) {
            entry.assetId = assetId;
            return this;
        }

        public Builder agreementId(String agreementId) {
            entry.agreementId = agreementId;
            return this;
        }

        public Builder transferProcessId(String transferProcessId) {
            entry.transferProcessId = transferProcessId;
            return this;
        }

        public EndpointDataReferenceEntry build() {
            requireNonNull(entry.assetId, "assetId");
            requireNonNull(entry.agreementId, "agreementId");
            requireNonNull(entry.transferProcessId, "transferProcessId");
            return entry;
        }
    }

}
