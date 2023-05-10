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

package org.eclipse.tractusx.edc.cp.adapter.callback;

import org.eclipse.edc.connector.contract.spi.event.contractnegotiation.ContractNegotiationFinalized;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.spi.callback.CallbackEventRemoteMessage;
import org.eclipse.edc.connector.transfer.spi.event.TransferProcessStarted;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class TestFunctions {

    public static ContractNegotiationFinalized getNegotiationFinalizedEvent() {
        var agreement = ContractAgreement.Builder.newInstance()
                .id("id")
                .policy(Policy.Builder.newInstance().build())
                .assetId("assetId")
                .consumerId("consumer")
                .providerId("provider")
                .build();

        return ContractNegotiationFinalized.Builder.newInstance()
                .contractNegotiationId("id")
                .protocol("test-protocol")
                .counterPartyId("counter-party")
                .counterPartyAddress("https://counter-party")
                .contractAgreement(agreement)
                .callbackAddresses(List.of(CallbackAddress.Builder.newInstance()
                        .uri("local://test")
                        .events(Set.of("test"))
                        .transactional(true)
                        .build()))
                .build();
    }

    public static TransferProcessStarted getTransferProcessStartedEvent() {
        return getTransferProcessStartedEvent(null);
    }

    public static TransferProcessStarted getTransferProcessStartedEvent(DataAddress dataAddress) {
        return TransferProcessStarted.Builder.newInstance()
                .callbackAddresses(List.of(CallbackAddress.Builder.newInstance()
                        .uri("local://test")
                        .events(Set.of("test"))
                        .transactional(true)
                        .build()))
                .dataAddress(dataAddress)
                .transferProcessId(UUID.randomUUID().toString())
                .build();
    }

    public static EndpointDataReference getEdr() {
        return EndpointDataReference.Builder.newInstance()
                .id("dataRequestId")
                .authCode("authCode")
                .authKey("authKey")
                .endpoint("http://endpoint")
                .build();
    }

    public static <T extends Event> CallbackEventRemoteMessage<T> remoteMessage(T event) {
        var callback = CallbackAddress.Builder.newInstance()
                .events(Set.of("test"))
                .uri("local://test")
                .build();

        var envelope = EventEnvelope.Builder
                .newInstance()
                .id(UUID.randomUUID().toString())
                .at(System.currentTimeMillis())
                .payload(event)
                .build();
        return new CallbackEventRemoteMessage<T>(callback, envelope, "local");
    }
}
