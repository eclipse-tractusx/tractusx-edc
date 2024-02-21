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

public final class CoreConstants {

    public static final String TX_PREFIX = "tx";
    public static final String TX_NAMESPACE = "https://w3id.org/tractusx/v0.0.1/ns/";
    public static final String TX_CONTEXT = "https://w3id.org/tractusx/edc/v0.0.1";
    public static final String EDC_CONTEXT = "https://w3id.org/edc/v0.0.1";
    public static final String CX_CREDENTIAL_NS = "https://w3id.org/catenax/credentials/";
    public static final String CX_POLICY_NS = "https://w3id.org/catenax/policy/";
    public static final String TX_CREDENTIAL_NAMESPACE = "https://w3id.org/tractusx/credentials/v0.0.1/ns/";

    private CoreConstants() {
    }
}
