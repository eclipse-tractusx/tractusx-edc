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

package org.eclipse.tractusx.edc.iam.ssi.miw;

import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.tractusx.edc.iam.ssi.miw.api.MiwApiClient;
import org.eclipse.tractusx.edc.iam.ssi.miw.credentials.SsiMiwCredentialClient;
import org.eclipse.tractusx.edc.iam.ssi.spi.SsiCredentialClient;

@Extension(SsiMiwCredentialClientExtension.EXTENSION_NAME)
public class SsiMiwCredentialClientExtension implements ServiceExtension {

    public static final String EXTENSION_NAME = "SSI MIW Credential Client";

    @Inject
    private MiwApiClient apiClient;

    @Inject
    private JsonLd jsonLdService;

    @Inject
    private Monitor monitor;

    @Override
    public String name() {
        return EXTENSION_NAME;
    }


    @Provider
    public SsiCredentialClient credentialVerifier() {
        return new SsiMiwCredentialClient(apiClient, jsonLdService, monitor);
    }


}
