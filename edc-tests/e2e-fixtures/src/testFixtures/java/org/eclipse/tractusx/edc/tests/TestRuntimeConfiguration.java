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

package org.eclipse.tractusx.edc.tests;

public class TestRuntimeConfiguration {

    public static final String BPN_SUFFIX = "-BPN";
    public static final String BPN_PREFIX = "BPNL0000";
    public static final String DID_PREFIX = "did:web:";
    public static final String CONSUMER_NAME = "CONSUMER";
    public static final String CONSUMER_BPN = "BPNL0000CONSUMER";
    public static final String CONSUMER_DID = DID_PREFIX + CONSUMER_NAME;
    public static final String PROVIDER_NAME = "PROVIDER";
    public static final String PROVIDER_BPN = "BPNL0000PROVIDER";
    public static final String PROVIDER_DID = DID_PREFIX + PROVIDER_NAME;
    public static final String DSP_08 = "dataspace-protocol-http";
    public static final String DSP_2025 = "dataspace-protocol-http:2025-1";
    public static final String DSP_2025_PATH = "/2025-1";

}
