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
 * Defines the summary context.
 */
public interface SummaryContext {
    String SUMMARY_CONTEXT = """
            {
              "@context": {
                "@version": 1.1,
                "@protected": true,
                "summary": "https://w3id.org/2023/catenax/credentials/summary/",
                "id": "@id",
                "type": "@type",
                "SummaryCredential" : {
                    "@id":"summary:SummaryCredential"
                },
                "holderIdentifier": {
                  "@id": "summary:holderIdentifier"
                },
                "name": {
                  "@id": "summary:name",
                  "@type": "https://schema.org/Text"
                },
                "items": {
                  "@id": "summary:items",
                  "@type": "https://schema.org/Text"
                },
                "contract-template": {
                  "@id": "summary:contract-template",
                  "@type": "https://schema.org/Text"
                }
              }
            }""";
}
