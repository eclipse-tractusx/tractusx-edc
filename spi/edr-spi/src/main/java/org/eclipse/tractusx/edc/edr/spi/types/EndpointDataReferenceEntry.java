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

package org.eclipse.tractusx.edc.edr.spi.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.spi.entity.StatefulEntity;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Predicate;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.TX_NAMESPACE;
import static org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntryStates.DELETING;
import static org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntryStates.ERROR;
import static org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntryStates.EXPIRED;
import static org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntryStates.NEGOTIATED;
import static org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntryStates.REFRESHING;

/**
 * An entry in the cache for an {@link EndpointDataReference}.
 */
@JsonDeserialize(builder = EndpointDataReferenceEntry.Builder.class)
public class EndpointDataReferenceEntry extends StatefulEntity<EndpointDataReferenceEntry> {

    public static final String SIMPLE_TYPE = "EndpointDataReferenceEntry";
    public static final String EDR_ENTRY_TYPE = TX_NAMESPACE + SIMPLE_TYPE;
    public static final String EDR_ENTRY_STATE = TX_NAMESPACE + "edrState";
    public static final String EDR_ENTRY_EXPIRATION_DATE = TX_NAMESPACE + "expirationDate";
    public static final String ASSET_ID = "assetId";
    public static final String EDR_ENTRY_ASSET_ID = EDC_NAMESPACE + ASSET_ID;
    public static final String AGREEMENT_ID = "agreementId";
    public static final String EDR_ENTRY_AGREEMENT_ID = EDC_NAMESPACE + AGREEMENT_ID;

    public static final String CONTRACT_NEGOTIATION_ID = "contractNegotiationId";

    public static final String EDR_ENTRY_CONTRACT_NEGOTIATION_ID = EDC_NAMESPACE + CONTRACT_NEGOTIATION_ID;

    public static final String TRANSFER_PROCESS_ID = "transferProcessId";
    public static final String EDR_ENTRY_TRANSFER_PROCESS_ID = EDC_NAMESPACE + TRANSFER_PROCESS_ID;
    public static final String PROVIDER_ID = "providerId";
    public static final String EDR_ENTRY_PROVIDER_ID = EDC_NAMESPACE + PROVIDER_ID;
    private String assetId;
    private String agreementId;
    private String transferProcessId;

    private String contractNegotiationId;

    private String providerId;

    private Long expirationTimestamp;

    private EndpointDataReferenceEntry() {
        state = NEGOTIATED.code();
    }

    @Override
    public String getId() {
        return getTransferProcessId();
    }


    @Override
    public EndpointDataReferenceEntry copy() {
        var builder = Builder.newInstance()
                .transferProcessId(transferProcessId)
                .agreementId(agreementId)
                .assetId(assetId)
                .providerId(providerId)
                .contractNegotiationId(contractNegotiationId)
                .expirationTimestamp(expirationTimestamp);
        return copy(builder);
    }

    @Override
    public String stateAsString() {
        return EndpointDataReferenceEntryStates.from(state).toString();
    }

    @JsonIgnore
    public String getEdrState() {
        return EndpointDataReferenceEntryStates.from(getState()).name();
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

    public String getProviderId() {
        return providerId;
    }

    public String getContractNegotiationId() {
        return contractNegotiationId;
    }

    public Long getExpirationTimestamp() {
        return expirationTimestamp;
    }

    @Override
    public int hashCode() {
        return Objects.hash(assetId, agreementId, transferProcessId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        var that = (EndpointDataReferenceEntry) o;

        return transferProcessId.equals(that.transferProcessId);
    }

    public void transitionToRefreshing() {
        transition(REFRESHING, REFRESHING, NEGOTIATED);
    }

    public void transitionToNegotiated() {
        transition(NEGOTIATED, NEGOTIATED, REFRESHING);
    }

    public void transitionError() {
        transition(ERROR, REFRESHING, NEGOTIATED);
    }

    public void transitionToExpired() {
        transition(EXPIRED, EXPIRED, NEGOTIATED, REFRESHING);
    }

    public void transitionToDeleting() {
        transition(DELETING, DELETING, EXPIRED);
    }

    private void transition(EndpointDataReferenceEntryStates end, Predicate<EndpointDataReferenceEntryStates> canTransitTo) {
        if (!canTransitTo.test(EndpointDataReferenceEntryStates.from(state))) {
            throw new IllegalStateException(format("Cannot transition from state %s to %s", EndpointDataReferenceEntryStates.from(state), EndpointDataReferenceEntryStates.from(end.code())));
        }
        transitionTo(end.code());
    }

    private void transition(EndpointDataReferenceEntryStates end, EndpointDataReferenceEntryStates... starts) {
        transition(end, (state) -> Arrays.stream(starts).anyMatch(s -> s == state));
    }


    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends StatefulEntity.Builder<EndpointDataReferenceEntry, Builder> {

        private Builder() {
            super(new EndpointDataReferenceEntry());
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder assetId(String assetId) {
            entity.assetId = assetId;
            return this;
        }

        public Builder agreementId(String agreementId) {
            entity.agreementId = agreementId;
            return this;
        }

        public Builder transferProcessId(String transferProcessId) {
            entity.transferProcessId = transferProcessId;
            entity.id = transferProcessId;
            return this;
        }

        public Builder providerId(String providerId) {
            entity.providerId = providerId;
            return this;
        }

        public Builder contractNegotiationId(String contractNegotiationId) {
            entity.contractNegotiationId = contractNegotiationId;
            return this;
        }

        public Builder expirationTimestamp(Long expirationTimestamp) {
            entity.expirationTimestamp = expirationTimestamp;
            return this;
        }

        @Override
        public Builder self() {
            return this;
        }

        public EndpointDataReferenceEntry build() {
            super.build();
            requireNonNull(entity.assetId, ASSET_ID);
            requireNonNull(entity.agreementId, AGREEMENT_ID);
            requireNonNull(entity.transferProcessId, TRANSFER_PROCESS_ID);

            return entity;
        }
    }

}
