/*
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft
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

package org.eclipse.tractusx.edc.identity.mapper;

public class TestData {
    public static final String MEMBERSHIP_CREDENTIAL = """
            {
              "@context": [
                "https://www.w3.org/2018/credentials/v1",
                "https://w3id.org/catenax/credentials/v1.0.0"
              ],
              "id": "1f36af58-0fc0-4b24-9b1c-e37d59668089",
              "type": [
                "VerifiableCredential",
                "MembershipCredential"
              ],
              "issuer": "did:web:com.example.issuer",
              "issuanceDate": "2021-06-16T18:56:59Z",
              "expirationDate": "2199-06-16T18:56:59Z",
              "credentialSubject": {
                "id": "%s",
                "holderIdentifier": "BPNL000000001"
              }
            }
            """;

    public static final String MEMBERSHIP_CREDENTIAL_EXPIRED = """
            {
              "@context": [
                "https://www.w3.org/2018/credentials/v1",
                "https://w3id.org/catenax/credentials/v1.0.0"
              ],
              "id": "1f36af58-0fc0-4b24-9b1c-e37d59668089",
              "type": [
                "VerifiableCredential",
                "MembershipCredential"
              ],
              "issuer": "did:web:com.example.issuer",
              "issuanceDate": "2021-06-16T18:56:59Z",
              "expirationDate": "2009-06-16T18:56:59Z",
              "credentialSubject": {
                "id": "%s",
                "holderIdentifier": "BPNL000000001"
              }
            }
            """;

    public static final String SOME_OTHER_CREDENTIAL = """
            {
              "@context": [
                "https://www.w3.org/2018/credentials/v1",
                "https://w3id.org/catenax/credentials/v1.0.0"
              ],
              "id": "1f36af58-0fc0-4b24-9b1c-e37d59668089",
              "type": [
                "VerifiableCredential",
                "SomeOtherCredential"
              ],
              "issuer": "did:web:com.example.issuer",
              "issuanceDate": "2021-06-16T18:56:59Z",
              "expirationDate": "2099-06-16T18:56:59Z",
              "credentialSubject": {
                "id": "%s",
                "holderIdentifier": "BPNL000000001"
              }
            }
            """;

    public static final String VP_CONTENT_EXAMPLE = """
                        {
                            "@context": [
                              "https://www.w3.org/2018/credentials/v1",
                              "https://www.w3.org/2018/credentials/examples/v1"
                            ],
                            "id": "https://exapmle.com/test-vp",
                            "holder": "%s",
                            "type": [
                              "VerifiablePresentation"
                            ],
                            "verifiableCredential": [
                              %s
                            ]
                        }
            """;
}
