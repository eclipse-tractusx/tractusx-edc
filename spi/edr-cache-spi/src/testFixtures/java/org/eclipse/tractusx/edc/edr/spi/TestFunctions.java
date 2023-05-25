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

import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;

public class TestFunctions {


    public static EndpointDataReference edr(String id) {
        return EndpointDataReference.Builder.newInstance()
                .endpoint("http://test.com")
                .id(id)
                .authCode("11111")
                .authKey("authentication").build();
    }

    public static EndpointDataReferenceEntry edrEntry(String assetId, String agreementId, String transferProcessId) {
        return EndpointDataReferenceEntry.Builder.newInstance()
                .assetId(assetId)
                .agreementId(agreementId)
                .transferProcessId(transferProcessId)
                .build();
    }
}
