/*
 * Copyright (c) 2026 Materna SE
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
 */

package org.constructx.edc.policy.constructx;

/**
 * Constants used across Construct-X EDC policy extension.
 */
public final class ConstructxPolicyConstants {

    public static final String CONSTRUCTX_POLICY_NS = "https://w3id.org/constructx/policy/v1.0/";
    public static final String CONSTRUCTX_POLICY_PREFIX = "constructx-policy";
    public static final String CONSTRUCTX_POLICY_CONTEXT = CONSTRUCTX_POLICY_NS + "context.jsonld";

    public static final String CONSTRUCTX_CONTEXT = "https://w3id.org/constructx/edc/v0.0.1";

    public static final String CONSTRUCTX_CREDENTIAL_NS = "https://w3id.org/constructx/credentials/v1.0/";
    public static final String CONSTRUCTX_CREDENTIAL_PREFIX = "constructx-credentials";
    public static final String CONSTRUCTX_CREDENTIAL_CONTEXT = CONSTRUCTX_CREDENTIAL_NS + "context.jsonld";

    private ConstructxPolicyConstants() {
    }
}