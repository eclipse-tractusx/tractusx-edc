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

package org.eclipse.tractusx.edc.edr.spi;

import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntry;
import org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntryStates;

import java.util.UUID;

import static org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntryStates.NEGOTIATED;

public class TestFunctions {


    public static EndpointDataReference edr(String id) {
        return EndpointDataReference.Builder.newInstance()
                .endpoint("http://test.com")
                .contractId("test-contract-id")
                .id(id)
                .authCode("11111")
                .authKey("authentication").build();
    }

    public static EndpointDataReferenceEntry edrEntry(String assetId, String agreementId, String transferProcessId) {
        return edrEntry(assetId, agreementId, transferProcessId, NEGOTIATED);
    }

    public static EndpointDataReferenceEntry edrEntry(String assetId, String agreementId, String transferProcessId, String contractNegotiationId) {
        return edrEntry(assetId, agreementId, transferProcessId, NEGOTIATED, contractNegotiationId);
    }

    public static EndpointDataReferenceEntry edrEntry(String assetId, String agreementId, String transferProcessId, EndpointDataReferenceEntryStates state) {
        return edrEntry(assetId, agreementId, transferProcessId, state, null);
    }

    public static EndpointDataReferenceEntry edrEntry(String assetId, String agreementId, String transferProcessId, EndpointDataReferenceEntryStates state, String contractNegotiationId) {
        return EndpointDataReferenceEntry.Builder.newInstance()
                .assetId(assetId)
                .agreementId(agreementId)
                .transferProcessId(transferProcessId)
                .providerId(UUID.randomUUID().toString())
                .contractNegotiationId(contractNegotiationId)
                .state(state.code())
                .build();
    }
}
