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

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.eclipse.edc.api.model.CallbackAddressDto;
import org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractOfferDescription;

import java.util.ArrayList;
import java.util.List;

public class TransferOpenRequestDto {

    @NotBlank(message = "connectorAddress is mandatory")
    private String connectorAddress;
    @NotBlank(message = "protocol is mandatory")
    private String protocol = "ids-multipart";
    @NotBlank(message = "connectorId is mandatory")
    private String connectorId;

    private String providerId;

    @NotNull(message = "offer cannot be null")
    private ContractOfferDescription offer;
    private List<CallbackAddressDto> callbackAddresses = new ArrayList<>();

    private TransferOpenRequestDto() {

    }

    public String getConnectorAddress() {
        return connectorAddress;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getConnectorId() {
        return connectorId;
    }

    public String getProviderId() {
        return providerId;
    }

    public List<CallbackAddressDto> getCallbackAddresses() {
        return callbackAddresses;
    }

    public ContractOfferDescription getOffer() {
        return offer;
    }
    
    public static final class Builder {
        private final TransferOpenRequestDto dto;

        private Builder() {
            dto = new TransferOpenRequestDto();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder connectorAddress(String connectorAddress) {
            dto.connectorAddress = connectorAddress;
            return this;
        }

        public Builder protocol(String protocol) {
            dto.protocol = protocol;
            return this;
        }

        public Builder connectorId(String connectorId) {
            dto.connectorId = connectorId;
            return this;
        }

        public Builder offer(ContractOfferDescription offer) {
            dto.offer = offer;
            return this;
        }

        public Builder providerId(String providerId) {
            dto.providerId = providerId;
            return this;
        }

        public Builder callbackAddresses(List<CallbackAddressDto> callbackAddresses) {
            dto.callbackAddresses = callbackAddresses;
            return this;
        }

        public TransferOpenRequestDto build() {
            return dto;
        }
    }
}
