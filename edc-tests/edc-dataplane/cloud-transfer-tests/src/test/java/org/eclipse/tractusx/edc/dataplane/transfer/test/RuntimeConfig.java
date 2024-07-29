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

package org.eclipse.tractusx.edc.dataplane.transfer.test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.eclipse.edc.util.io.Ports.getFreePort;

/**
 * Configuration baseline for Data-Plane e2e tests
 */
public class RuntimeConfig {
    /**
     * Configures the data plane token endpoint, and all relevant HTTP contexts
     */
    public static Map<String, String> baseConfig(String controlPath, int controlPort) {
        return new HashMap<>() {
            {
                put("web.http.path", "/api");
                put("web.http.port", String.valueOf(getFreePort()));
                put("web.http.control.path", controlPath);
                put("web.http.control.port", String.valueOf(controlPort));
                put("web.http.public.path", "/public");
                put("web.http.public.port", String.valueOf(getFreePort()));
                put("web.http.consumer.api.path", "/api/consumer");
                put("web.http.consumer.api.port", String.valueOf(getFreePort()));
                put("tx.dpf.consumer.proxy.port", String.valueOf(getFreePort()));
                put("edc.iam.issuer.id", "did:web:" + UUID.randomUUID());
                put("edc.iam.sts.oauth.token.url", "http://sts.example.com/token");
                put("edc.iam.sts.oauth.client.id", "test-clientid");
                put("edc.iam.sts.oauth.client.secret.alias", "test-clientid-alias");
                put("tx.edc.iam.sts.dim.url", "http://sts.example.com");
                put("tx.edc.iam.iatp.bdrs.server.url", "http://sts.example.com");
                put("edc.transfer.proxy.token.verifier.publickey.alias", "not-used-but-mandatory");
                put("edc.transfer.proxy.token.signer.privatekey.alias", "not-used-but-mandatory");
            }
        };
    }

    /**
     * Azure specific configuration, e.g. access credentials, blobstore endpoint templates, etc.
     */
    public static class Azure {
        /**
         * Creates a configuration for a Provider runtime, running Azure ingress and egress
         *
         * @param controlPath       the controlPath of the control API
         * @param controlPort       the port of the control API
         * @param mappedAzuritePort the host port for the Blob endpoint template.
         */
        public static Map<String, String> blobstoreDataplaneConfig(String controlPath, int controlPort, Integer mappedAzuritePort) {
            var base = baseConfig(controlPath, controlPort);

            base.putAll(new HashMap<>() {
                {
                    put("edc.blobstore.endpoint.template", "http://127.0.0.1:" + mappedAzuritePort + "/%s");
                }
            });
            return base;
        }
    }

    public static class S3 {

        public static Map<String, String> s3dataplaneConfig(String controlPath, int controlPort) {
            return baseConfig(controlPath, controlPort);
        }
    }
}

