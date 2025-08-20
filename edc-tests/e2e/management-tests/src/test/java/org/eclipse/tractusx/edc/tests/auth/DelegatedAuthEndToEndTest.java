/*
 * Copyright (c) 2025 Cofinity-X
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
 */

package org.eclipse.tractusx.edc.tests.auth;

import io.restassured.http.ContentType;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.tractusx.edc.tests.participant.TransferParticipant;
import org.eclipse.tractusx.edc.tests.runtimes.PostgresExtension;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;

import static org.eclipse.tractusx.edc.tests.runtimes.Runtimes.pgRuntime;

@EndToEndTest
public class DelegatedAuthEndToEndTest {

    private static final TransferParticipant CONNECTOR = TransferParticipant.Builder.newInstance()
            .name("connector")
            .id("did:web:connector")
            .bpn("connector-bpn")
            .build();

    @RegisterExtension
    @Order(0)
    private static final PostgresExtension POSTGRES = new PostgresExtension(CONNECTOR.getName());

    @RegisterExtension
    @Order(1)
    private static final KeycloakExtension KEYCLOAK = new KeycloakExtension();

    @RegisterExtension
    private static final RuntimeExtension RUNTIME = pgRuntime(CONNECTOR, POSTGRES,
            () -> CONNECTOR.getConfig().merge(ConfigFactory.fromMap(Map.of(
                            "web.http.management.auth.type", "delegated",
                            "web.http.management.auth.dac.audience", KEYCLOAK.audience(),
                            "web.http.management.auth.dac.key.url", KEYCLOAK.jwksUrl()
            )))
    );

    @Test
    void shouldDelegateAuth() {
        var token = KEYCLOAK.issueToken();

        CONNECTOR.baseManagementRequest()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .post("/v3/assets/request")
                .then()
                .log().ifValidationFails()
                .statusCode(200);
    }
}
