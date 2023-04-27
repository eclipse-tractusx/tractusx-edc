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

package org.eclipse.tractusx.edc.api.cp.adapter;

import org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractOfferDescription;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.tractusx.edc.api.cp.adapter.dto.TransferOpenRequestDto;
import org.eclipse.tractusx.edc.spi.cp.adapter.types.TransferOpenRequest;

import java.util.UUID;

public class TestFunctions {

    public static ContractOfferDescription createOffer(String offerId, String assetId) {
        return ContractOfferDescription.Builder.newInstance()
                .offerId(offerId)
                .assetId(assetId)
                .policy(Policy.Builder.newInstance().build())
                .build();
    }

    public static ContractOfferDescription createOffer(Policy policy) {
        return ContractOfferDescription.Builder.newInstance()
                .offerId(UUID.randomUUID().toString())
                .assetId(UUID.randomUUID().toString())
                .policy(policy)
                .build();
    }

    public static ContractOfferDescription createOffer(String offerId) {
        return createOffer(offerId, UUID.randomUUID().toString());
    }

    public static ContractOfferDescription createOffer() {
        return createOffer(UUID.randomUUID().toString(), UUID.randomUUID().toString());
    }

    public static TransferOpenRequestDto requestDto() {
        return TransferOpenRequestDto.Builder.newInstance()
                .connectorAddress("test")
                .connectorId("id")
                .protocol("test-protocol")
                .offer(ContractOfferDescription.Builder.newInstance()
                        .offerId("offerId")
                        .assetId("assetId")
                        .policy(Policy.Builder.newInstance().build()).build())
                .build();
    }

    public static TransferOpenRequest openRequest() {
        return TransferOpenRequest.Builder.newInstance()
                .connectorAddress("test")
                .connectorId("id")
                .protocol("test-protocol")
                .offer(ContractOffer.Builder.newInstance()
                        .id("offerId")
                        .asset(Asset.Builder.newInstance().id("assetId").build())
                        .policy(Policy.Builder.newInstance().build()).build())
                .build();
    }
}
