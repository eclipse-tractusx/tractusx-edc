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

package org.eclipse.tractusx.edc.policy.cx.summary;

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
                  "issuer": "did:web:a016-203-129-213-99.ngrok-free.app:BPNL000000000000",
                  "issuanceDate": "2023-06-02T12:00:00Z",
                  "expirationDate": "2022-06-16T18:56:59Z",
                  "credentialSubject": {
                    "id": "did:web:a016-203-129-213-99.ngrok-free.app:BPNL000000000000",
                    "holderIdentifier": "BPN of holder",
                    "type": "Summary-List",
                    "name": "CX-Credentials",
                    "items": [
                      "cx-active-member",
                      "cx-dismantler",
                      "cx-pcf",
                      "cx-sustainability",
                      "cx-quality",
                      "cx-traceability",
                      "cx-behavior-twin",
                      "cx-bpn"
                    ],
                    "contract-templates": "https://public.catena-x.org/contracts/"
                  },
                  "proof": {
                    "type": "Ed25519Signature2018",
                    "created": "2023-06-02T12:00:00Z",
                    "proofPurpose": "assertionMethod",
                    "verificationMethod": "did:web:example.com#key-1",
                    "jws": "eyJhbGciOiJFZERTQSJ9.eyJpYXQiOjE2MjM1NzA3NDEsImV4cCI6MTYyMzU3NDM0MSwianRpIjoiMTIzNDU2NzgtMTIzNC0xMjM0LTEyMzQtMTIzNDU2Nzg5YWJjIiwicHJvb2YiOnsiaWQiOiJkaWQ6d2ViOmV4YW1wbGUuY29tIiwibmFtZSI6IkJlaXNwaWVsLU9yZ2FuaXNhdGlvbiJ9fQ.SignedExampleSignature"
                  }
                }
              ]
            }
             """;
}
