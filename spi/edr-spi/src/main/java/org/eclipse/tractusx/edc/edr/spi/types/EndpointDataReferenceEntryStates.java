/*
 *  Copyright (c) 2021 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Fraunhofer Institute for Software and Systems Engineering - minor modifications
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.tractusx.edc.edr.spi.types;

import java.util.Arrays;

/**
 * Defines the states an EDR entry can be in.
 */
public enum EndpointDataReferenceEntryStates {

    NEGOTIATED(50),

    REFRESHING(100),

    EXPIRED(200),
    ERROR(300),

    DELETED(400);


    private final int code;

    EndpointDataReferenceEntryStates(int code) {
        this.code = code;
    }

    public static EndpointDataReferenceEntryStates from(int code) {
        return Arrays.stream(values()).filter(tps -> tps.code == code).findFirst().orElse(null);
    }

    public int code() {
        return code;
    }

}
