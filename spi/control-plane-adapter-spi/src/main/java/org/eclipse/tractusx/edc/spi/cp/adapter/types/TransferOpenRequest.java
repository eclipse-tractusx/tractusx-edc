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

package org.eclipse.tractusx.edc.spi.cp.adapter.types;

import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TransferOpenRequest {

    private String connectorAddress;
    private String protocol = "ids-multipart";
    private String connectorId;
    private ContractOffer offer;
    private String providerId;
    private String consumerId;

    private List<CallbackAddress> callbackAddresses = new ArrayList<>();

    private TransferOpenRequest() {

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


    public String getConsumerId() {
        return consumerId;
    }

    public String getProviderId() {
        return providerId;
    }

    public List<CallbackAddress> getCallbackAddresses() {
        return callbackAddresses;
    }

    public ContractOffer getOffer() {
        return offer;
    }


    public static final class Builder {
        private final TransferOpenRequest entity;

        private Builder() {
            entity = new TransferOpenRequest();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder connectorAddress(String connectorAddress) {
            entity.connectorAddress = connectorAddress;
            return this;
        }

        public Builder protocol(String protocol) {
            entity.protocol = protocol;
            return this;
        }

        public Builder connectorId(String connectorId) {
            entity.connectorId = connectorId;
            return this;
        }

        public Builder offer(ContractOffer offer) {
            entity.offer = offer;
            return this;
        }


        public Builder consumerId(String consumerId) {
            entity.consumerId = consumerId;
            return this;
        }

        public Builder providerId(String providerId) {
            entity.providerId = providerId;
            return this;
        }

        public Builder callbackAddresses(List<CallbackAddress> callbackAddresses) {
            entity.callbackAddresses = callbackAddresses;
            return this;
        }

        public TransferOpenRequest build() {
            Objects.requireNonNull(entity.protocol, "protocol should not be null");
            Objects.requireNonNull(entity.connectorAddress, "connector address should not be null");
            Objects.requireNonNull(entity.offer, "offer should not be null");
            return entity;
        }
    }
}
