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

package org.eclipse.tractusx.edc.iam.ssi.spi.jsonld;

/**
 * Defines policy namespaces.
 */
public interface CredentialsNamespaces {

    String W3C_VC_PREFIX = "https://www.w3.org/2018/credentials";
    String W3C_VC_NS = W3C_VC_PREFIX + "/v1";
    String VP_PROPERTY = "vp";
    @Deprecated
    String CX_NS = "https://w3id.org/2023/catenax/credentials/";
    @Deprecated
    String CX_SUMMARY_NS = CX_NS + "summary";
    @Deprecated
    String CX_SUMMARY_NS_V1 = CX_SUMMARY_NS + "/v1";
    @Deprecated
    String SUMMARY_CREDENTIAL_TYPE = CX_SUMMARY_NS + "/SummaryCredential";
    @Deprecated
    String HOLDER_IDENTIFIER = CX_SUMMARY_NS + "/holderIdentifier";
    @Deprecated
    String CX_USE_CASE_NS = CX_NS + "usecase";
    @Deprecated
    String CX_USE_CASE_NS_V1 = CX_USE_CASE_NS + "/v1";
    @Deprecated
    String CX_SUMMARY_CREDENTIAL = "SummaryCredential";
    String CREDENTIAL_SUBJECT = W3C_VC_PREFIX + "#credentialSubject";
    String CREDENTIAL_ISSUER = W3C_VC_PREFIX + "#issuer";

}
