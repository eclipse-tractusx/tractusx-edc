/********************************************************************************
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.iatp;

import com.networknt.schema.format.PatternFormat;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import java.net.URISyntaxException;

@Extension("Credentials JSON LD extension")
public class CredentialsJsonLdExtension implements ServiceExtension {

    public static final String BUSINESS_PARTNER_DATA = "https://w3id.org/catenax/credentials";

    @Inject
    private JsonLd jsonLd;

    @Override
    public void initialize(ServiceExtensionContext context) {

        try {
            jsonLd.registerCachedDocument(BUSINESS_PARTNER_DATA, Thread.currentThread().getContextClassLoader().getResource("cx-credentials-context.json").toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        // TODO: test
        try {
            getClass().getClassLoader().loadClass(PatternFormat.class.getName());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
