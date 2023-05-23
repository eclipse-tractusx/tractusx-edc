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

package org.eclipse.tractusx.edc.api.cp.adapter.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.TX_NAMESPACE;


public class EndpointDataReferenceEntryDto {

    public static final String SIMPLE_TYPE = "EndpointDataReferenceEntryDto";

    public static final String EDR_ENTRY_DTO_TYPE = TX_NAMESPACE + SIMPLE_TYPE;
    public static final String EDR_ENTRY_DTO_ASSET_ID = EDC_NAMESPACE + "assetId";
    public static final String EDR_ENTRY_DTO_AGREEMENT_ID = EDC_NAMESPACE + "agreementId";
    public static final String EDR_ENTRY_TRANSFER_PROCESS_ID = EDC_NAMESPACE + "transferProcessId";

    private String assetId;
    private String agreementId;
    private String transferProcessId;

    private EndpointDataReferenceEntryDto() {
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


    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private final EndpointDataReferenceEntryDto entry;

        private Builder() {
            entry = new EndpointDataReferenceEntryDto();
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

        public EndpointDataReferenceEntryDto build() {
            return entry;
        }
    }

}
