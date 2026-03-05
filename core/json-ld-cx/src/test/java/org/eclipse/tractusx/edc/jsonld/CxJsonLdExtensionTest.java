/********************************************************************************
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 * Copyright (c) 2026 SAP SE
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

package org.eclipse.tractusx.edc.jsonld;

import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.tractusx.edc.cx.CxJsonLdExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.URI;

import static org.eclipse.tractusx.edc.cx.CxJsonLdExtension.CX_ODRL_CONTEXT;
import static org.eclipse.tractusx.edc.cx.CxJsonLdExtension.CX_POLICY_2025_09_CONTEXT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;

@ExtendWith(DependencyInjectionExtension.class)
public class CxJsonLdExtensionTest {

    JsonLd jsonLdService = mock(JsonLd.class);

    @BeforeEach
    void setup(ServiceExtensionContext context) {
        context.registerService(JsonLd.class, jsonLdService);
    }

    @Test
    void initialize(ServiceExtensionContext context, CxJsonLdExtension extension) {
        extension.initialize(context);
        jsonLdService.registerCachedDocument(eq(CX_POLICY_2025_09_CONTEXT), any(URI.class));
        jsonLdService.registerCachedDocument(eq(CX_ODRL_CONTEXT), any(URI.class));
    }
}
