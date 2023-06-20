/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.tractusx.edc.iam.ssi.identity.extractor.fixtures;

public interface Credentials {

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
                  "issuer": "did:web:a016-203-129-213-99.ngrok-free.app:BPNL000000000000",
                  "issuanceDate": "2023-06-02T12:00:00Z",
                  "expirationDate": "2022-06-16T18:56:59Z",
                  "credentialSubject": {
                    "id": "did:web:a016-203-129-213-99.ngrok-free.app:BPNL000000000000"     
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
                  "issuer": "did:web:a016-203-129-213-99.ngrok-free.app:BPNL000000000000",
                  "issuanceDate": "2023-06-02T12:00:00Z",
                  "expirationDate": "2022-06-16T18:56:59Z",
                  "credentialSubject": {
                    "id": "did:web:a016-203-129-213-99.ngrok-free.app:BPNL000000000000"     
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
                  "issuer": "did:web:a016-203-129-213-99.ngrok-free.app:BPNL000000000000",
                  "issuanceDate": "2023-06-02T12:00:00Z",
                  "expirationDate": "2022-06-16T18:56:59Z"
                }
              ]
            }
             """;
}
