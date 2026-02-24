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

package org.eclipse.tractusx.edc.cx;

import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import java.io.File;
import java.util.Map;

import static org.eclipse.edc.api.management.ManagementApi.MANAGEMENT_SCOPE;
import static org.eclipse.edc.protocol.dsp.spi.type.Dsp08Constants.DSP_SCOPE_V_08;
import static org.eclipse.edc.protocol.dsp.spi.type.Dsp2025Constants.DSP_SCOPE_V_2025_1;
import static org.eclipse.tractusx.edc.core.utils.FileUtils.getResourceFile;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.CX_POLICY_NS;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.CX_POLICY_PREFIX;

public class CxJsonLdExtension implements ServiceExtension {

    @Deprecated(since = "0.11.0")
    public static final String CX_POLICY_CONTEXT = "https://w3id.org/tractusx/policy/v1.0.0";
    public static final String CX_ODRL_CONTEXT = "https://w3id.org/catenax/2025/9/policy/odrl.jsonld";
    public static final String CX_POLICY_2025_09_CONTEXT = "https://w3id.org/catenax/2025/9/policy/context.jsonld";

    private static final String PREFIX = "document" + File.separator;
    private static final Map<String, String> FILES = Map.of(
            CX_POLICY_2025_09_CONTEXT, PREFIX + "cx-policy-v1.jsonld",
            CX_ODRL_CONTEXT, PREFIX + "cx-odrl.jsonld");
    @Inject
    private JsonLd jsonLdService;

    @Inject
    private Monitor monitor;

    @Override
    public void initialize(ServiceExtensionContext context) {
        jsonLdService.registerNamespace(CX_POLICY_PREFIX, CX_POLICY_NS, DSP_SCOPE_V_08);

        jsonLdService.registerContext(CX_POLICY_2025_09_CONTEXT, DSP_SCOPE_V_2025_1);
        jsonLdService.registerContext(CX_ODRL_CONTEXT, DSP_SCOPE_V_2025_1);

        jsonLdService.registerContext(CX_POLICY_2025_09_CONTEXT, MANAGEMENT_SCOPE);

        FILES.entrySet().stream().map(this::mapToFile)
                .forEach(result -> result.onSuccess(entry -> jsonLdService.registerCachedDocument(entry.getKey(), entry.getValue().toURI()))
                        .onFailure(failure -> monitor.warning("Failed to register cached json-ld document: " + failure.getFailureDetail())));
    }

    private Result<Map.Entry<String, File>> mapToFile(Map.Entry<String, String> fileEntry) {
        return getResourceFile(fileEntry.getValue())
                .map(file1 -> Map.entry(fileEntry.getKey(), file1));
    }
}
