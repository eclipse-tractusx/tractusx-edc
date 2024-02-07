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
 * Sample summary credential.
 */
public interface SummaryCredential {
    String SUMMARY_VP = """
            {
              "@context": [
                "https://www.w3.org/2018/credentials/v1"
              ],
              "type": "VerifiablePresentation",
              "verifiableCredential": [
                {
                  "@context": [
                    "https://www.w3.org/2018/credentials/v1",
                    "https://w3id.org/2023/catenax/credentials/summary/v1"
                  ],
                  "id": "urn:uuid:12345678-1234-1234-1234-123456789abc",
                  "type": [
                    "VerifiableCredential",
                    "SummaryCredential"
                  ],
                  "issuer": "did:web:issuer-example.com",
                  "issuanceDate": "2023-06-02T12:00:00Z",
                  "expirationDate": "2022-06-16T18:56:59Z",
                  "credentialSubject": {
                    "id": "did:web:example.com",
                    "holderIdentifier": "BPN of holder",
                    "type": "Summary-List",
                    "name": "CX-Credentials",
                    "items": [
                      "MembershipCredential",
                      "DismantlerCredential",
                      "PcfCredential",
                      "SustainabilityCredential",
                      "QualityCredential",
                      "TraceabilityCredential",
                      "BehaviorTwinCredential",
                      "BpnCredential"
                    ],
                    "contractTemplates": "https://public.catena-x.org/contracts/"
                  },
                  "proof": {
                    "type": "Ed25519Signature2018",
                    "created": "2023-06-02T12:00:00Z",
                    "proofPurpose": "assertionMethod",
                    "verificationMethod": "did:web:example.com#key-1",
                    "jws": "xxxx"
                  }
                }
              ]
            }
             """;

    String SIMPLE_VP = """
            {
              "@context": [
                "https://www.w3.org/2018/credentials/v1"
              ],
              "type": "VerifiablePresentation",
              "verifiableCredential": [
                {
                  "@context": [
                    "https://www.w3.org/2018/credentials/v1"
                  ],
                  "id": "urn:uuid:12345678-1234-1234-1234-123456789abc",
                  "type": [
                    "VerifiableCredential"
                  ],
                  "issuer": "did:web:example.com",
                  "issuanceDate": "2023-06-02T12:00:00Z",
                  "expirationDate": "2022-06-16T18:56:59Z",
                  "credentialSubject": {
                    "id": "did:web:example.com"     
                  }
                }
              ]
            }
             """;

    String SUMMARY_VP_NO_HOLDER = """
            {
              "@context": [
                "https://www.w3.org/2018/credentials/v1"
              ],
              "type": "VerifiablePresentation",
              "verifiableCredential": [
                {
                  "@context": [
                    "https://www.w3.org/2018/credentials/v1",
                    "https://w3id.org/2023/catenax/credentials/summary/v1"
                  ],
                  "id": "urn:uuid:12345678-1234-1234-1234-123456789abc",
                  "type": [
                    "VerifiableCredential",
                    "SummaryCredential"
                  ],
                  "issuer": "did:web:no-holder.example.com:BPNL000000000000",
                  "issuanceDate": "2023-06-02T12:00:00Z",
                  "expirationDate": "2022-06-16T18:56:59Z",
                  "credentialSubject": {
                    "id": "did:web:example.com"     
                  }
                }
              ]
            }
             """;

    String SUMMARY_VP_NO_SUBJECT = """
            {
              "@context": [
                "https://www.w3.org/2018/credentials/v1"
              ],
              "type": "VerifiablePresentation",
              "verifiableCredential": [
                {
                  "@context": [
                    "https://www.w3.org/2018/credentials/v1",
                    "https://w3id.org/2023/catenax/credentials/summary/v1"
                  ],
                  "id": "urn:uuid:12345678-1234-1234-1234-123456789abc",
                  "type": [
                    "VerifiableCredential",
                    "SummaryCredential"
                  ],
                  "issuer": "did:web:example.com",
                  "issuanceDate": "2023-06-02T12:00:00Z",
                  "expirationDate": "2022-06-16T18:56:59Z"
                }
              ]
            }
             """;
}
