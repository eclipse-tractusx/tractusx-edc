/*
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.tractusx.edc.lifecycle;


import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.HashMap;

import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.DSP_PATH;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.PLATO_CONNECTOR_PATH;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.PLATO_CONNECTOR_PORT;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.PLATO_DATAPLANE_CONTROL_PORT;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.PLATO_DSP_API_PORT;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.PLATO_DSP_CALLBACK;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.PLATO_MANAGEMENT_PATH;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.PLATO_MANAGEMENT_PORT;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.PLATO_PUBLIC_API_PORT;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.SOKRATES_CONNECTOR_PATH;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.SOKRATES_CONNECTOR_PORT;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.SOKRATES_DATAPLANE_CONTROL_PORT;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.SOKRATES_DSP_API_PORT;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.SOKRATES_DSP_CALLBACK;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.SOKRATES_MANAGEMENT_PATH;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.SOKRATES_MANAGEMENT_PORT;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.SOKRATES_PUBLIC_API_PORT;


public class MultiRuntimeTest {
    public static final String BPN_SUFFIX = "-BPN";
    public static final String SOKRATES_NAME = "SOKRATES";
    public static final String SOKRATES_BPN = SOKRATES_NAME + BPN_SUFFIX;

    @RegisterExtension
    protected static Participant sokrates = new Participant(
            ":edc-tests:runtime",
            SOKRATES_NAME,
            SOKRATES_BPN,
            new HashMap<>() {
                {
                    put("edc.connector.name", "sokrates");
                    put("edc.participant.id", SOKRATES_BPN);
                    put("web.http.port", String.valueOf(SOKRATES_CONNECTOR_PORT));
                    put("web.http.path", SOKRATES_CONNECTOR_PATH);
                    put("web.http.management.port", String.valueOf(SOKRATES_MANAGEMENT_PORT));
                    put("web.http.management.path", SOKRATES_MANAGEMENT_PATH);
                    put("web.http.protocol.port", String.valueOf(SOKRATES_DSP_API_PORT));
                    put("web.http.protocol.path", DSP_PATH);
                    put("edc.dsp.callback.address", SOKRATES_DSP_CALLBACK);
                    put("edc.api.auth.key", "testkey");
                    put("web.http.public.path", "/api/public");
                    put("web.http.public.port", SOKRATES_PUBLIC_API_PORT);

                    // embedded dataplane config
                    put("web.http.control.path", "/api/dataplane/control");
                    put("web.http.control.port", SOKRATES_DATAPLANE_CONTROL_PORT);
                    put("edc.dataplane.token.validation.endpoint", "http://localhost:" + SOKRATES_DATAPLANE_CONTROL_PORT + "/api/dataplane/control/token");
                    put("edc.dataplane.selector.httpplane.url", "http://localhost:" + SOKRATES_DATAPLANE_CONTROL_PORT + "/api/dataplane/control");
                    put("edc.dataplane.selector.httpplane.sourcetypes", "HttpData");
                    put("edc.dataplane.selector.httpplane.destinationtypes", "HttpProxy");
                    put("edc.dataplane.selector.httpplane.properties", "{\"publicApiUrl\":\"http://localhost:" + SOKRATES_PUBLIC_API_PORT + "/api/public\"}");
                    put("edc.receiver.http.dynamic.endpoint", "http://localhost:" + SOKRATES_CONNECTOR_PORT + "/api/consumer/datareference");
                    put("tractusx.businesspartnervalidation.log.agreement.validation", "true");
                    put("edc.agent.identity.key", "BusinessPartnerNumber");
                }
            });
    public static final String PLATO_NAME = "PLATO";
    public static final String PLATO_BPN = PLATO_NAME + BPN_SUFFIX;

    @RegisterExtension
    protected static Participant plato = new Participant(
            ":edc-tests:runtime",
            PLATO_NAME,
            PLATO_BPN,
            new HashMap<>() {
                {
                    put("edc.connector.name", "plato");
                    put("edc.participant.id", PLATO_BPN);
                    put("web.http.default.port", String.valueOf(PLATO_CONNECTOR_PORT));
                    put("web.http.default.path", PLATO_CONNECTOR_PATH);
                    put("web.http.management.port", String.valueOf(PLATO_MANAGEMENT_PORT));
                    put("web.http.management.path", PLATO_MANAGEMENT_PATH);
                    put("web.http.protocol.port", String.valueOf(PLATO_DSP_API_PORT));
                    put("web.http.protocol.path", DSP_PATH);
                    put("edc.dsp.callback.address", PLATO_DSP_CALLBACK);
                    put("edc.api.auth.key", "testkey");
                    put("web.http.public.port", PLATO_PUBLIC_API_PORT);
                    put("web.http.public.path", "/api/public");
                    // embedded dataplane config
                    put("web.http.control.path", "/api/dataplane/control");
                    put("web.http.control.port", PLATO_DATAPLANE_CONTROL_PORT);
                    put("edc.dataplane.token.validation.endpoint", "http://localhost:" + PLATO_DATAPLANE_CONTROL_PORT + "/api/dataplane/control/token");
                    put("edc.dataplane.selector.httpplane.url", "http://localhost:" + PLATO_DATAPLANE_CONTROL_PORT + "/api/dataplane/control");
                    put("edc.dataplane.selector.httpplane.sourcetypes", "HttpData");
                    put("edc.dataplane.selector.httpplane.destinationtypes", "HttpProxy");
                    put("edc.dataplane.selector.httpplane.properties", "{\"publicApiUrl\":\"http://localhost:" + PLATO_PUBLIC_API_PORT + "/api/public\"}");
                    put("tractusx.businesspartnervalidation.log.agreement.validation", "true");
                    put("edc.agent.identity.key", "BusinessPartnerNumber");
                }
            });
}
