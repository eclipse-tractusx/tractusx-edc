/********************************************************************************
 * Copyright (c) 2021,2022 Microsoft Corporation
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

    DELETING(400);
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
