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

package org.eclipse.tractusx.edc.tests.edr;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockWebServer;
import org.eclipse.tractusx.edc.helpers.ReceivedEvent;

import java.util.concurrent.TimeUnit;

public class TestFunctions {


    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static ReceivedEvent waitForEvent(MockWebServer server) {
        try {
            var request = server.takeRequest(60, TimeUnit.SECONDS);
            if (request != null) {
                return MAPPER.readValue(request.getBody().inputStream(), ReceivedEvent.class);
            } else {
                throw new RuntimeException("Timeout exceeded waiting for events");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
