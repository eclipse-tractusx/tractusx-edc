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

package org.eclipse.tractusx.edc.dataplane.tokenrefresh.e2e;

import java.util.HashMap;
import java.util.Map;

import static org.eclipse.edc.junit.testfixtures.TestUtils.getFreePort;

/**
 * Configuration baseline for Data-Plane e2e tests
 */
public class RuntimeConfig {
    /**
     * Configures the data plane token endpoint, and all relevant HTTP contexts
     */
    public static Map<String, String> baseConfig(String signalingPath, int signalingPort) {
        return new HashMap<>() {
            {
                put("edc.dataplane.token.validation.endpoint", "http://token-validation.com");
                put("web.http.path", "/api");
                put("web.http.port", String.valueOf(getFreePort()));
                put("web.http.public.path", "/public");
                put("web.http.public.port", String.valueOf(getFreePort()));
                put("web.http.consumer.api.path", "/api/consumer");
                put("web.http.consumer.api.port", String.valueOf(getFreePort()));
                put("web.http.signaling.path", signalingPath);
                put("web.http.signaling.port", String.valueOf(signalingPort));
            }
        };
    }

}

