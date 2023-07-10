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

package org.eclipse.tractusx.edc.dataplane.proxy.provider.api.response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.tractusx.edc.dataplane.proxy.provider.api.validation.ProxyProviderDataAddressResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getFreePort;
import static org.eclipse.edc.junit.testfixtures.TestUtils.testHttpClient;

public class ProxyProviderDataAddressResolverTest {

    private static final ObjectMapper MAPPER = new TypeManager().getMapper();
    private static final int PORT = getFreePort();
    private static final String TOKEN_VALIDATION_SERVER_URL = "http://localhost:" + PORT;
    private MockWebServer mockServer;

    private ProxyProviderDataAddressResolver resolver;

    @BeforeEach
    public void startServer() throws IOException {
        mockServer = new MockWebServer();
        mockServer.start(PORT);
        resolver = new ProxyProviderDataAddressResolver(testHttpClient(), TOKEN_VALIDATION_SERVER_URL, MAPPER);
    }

    @AfterEach
    public void stopServer() throws IOException {
        mockServer.shutdown();
    }
    
    @Test
    void verifySuccessTokenValidation() throws JsonProcessingException {
        var token = UUID.randomUUID().toString();
        var address = DataAddress.Builder.newInstance()
                .type("test-type")
                .build();

        mockServer.enqueue(new MockResponse().setBody(MAPPER.writeValueAsString(address)).setResponseCode(200));


        var result = resolver.resolve(token);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent().getType()).isEqualTo(address.getType());
    }

    @Test
    void verifyFailedResultReturnedIfServerResponseIsUnsuccessful() throws JsonProcessingException {
        var token = UUID.randomUUID().toString();

        mockServer.enqueue(new MockResponse().setResponseCode(400));

        var result = resolver.resolve(token);

        assertThat(result.failed()).isTrue();
    }
}
