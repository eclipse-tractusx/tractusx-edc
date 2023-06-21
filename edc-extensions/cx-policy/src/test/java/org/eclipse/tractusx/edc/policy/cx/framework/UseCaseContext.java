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

package org.eclipse.tractusx.edc.policy.cx.framework;

/**
 * Defines the context for use case credentials.
 */
public interface UseCaseContext {

    String USE_CASE_CONTEXT = """
            {
               "@context": {
                 "@version": 1.1,
                 "@protected": true,
                 "usecase": "https://w3id.org/2023/catenax/credentials/usecase/",
                 "id": "@id",
                 "type": "@type",
                 "usecaseAgreement": {
                   "@id": "usecase:usecaseAgreement",
                   "@context": {
                     "contractTemplate": {
                       "@id": "usecase:contractTemplate",
                       "@type": "https://schema.org/Text"
                     },
                     "contractVersion": {
                       "@id": "usecase:contractVersion",
                       "@type": "https://schema.org/Text"
                     }
                   }
                 }
               }
            }""";


}
