/********************************************************************************
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/

package org.eclipse.tractusx.edc.edr.core.fixtures;

import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.eclipse.edc.spi.types.domain.offer.ContractOffer;
import org.eclipse.tractusx.edc.edr.spi.types.NegotiateEdrRequest;

import java.util.List;
import java.util.Set;

public class TestFunctions {


    public static NegotiateEdrRequest getNegotiateEdrRequest() {
        return NegotiateEdrRequest.Builder.newInstance()
                .protocol("protocol")
                .connectorAddress("http://test")
                .callbackAddresses(List.of(CallbackAddress.Builder.newInstance().uri("test").events(Set.of("test")).build()))
                .offer(ContractOffer.Builder.newInstance()
                        .id("id")
                        .assetId("assetId")
                        .policy(Policy.Builder.newInstance().build())
                        .build())
                .build();
    }

    public static ContractNegotiation getContractNegotiation() {
        return ContractNegotiation.Builder.newInstance()
                .id("id")
                .counterPartyAddress("http://test")
                .counterPartyId("provider")
                .protocol("protocol")
                .build();
    }
}
