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

import dasniko.testcontainers.keycloak.KeycloakContainer;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;

public class KeycloakExtension implements BeforeAllCallback, AfterAllCallback {

    private final KeycloakContainer keycloak = new KeycloakContainer();
    private CredentialRepresentation clientSecret;
    private final String clientId = UUID.randomUUID().toString();

    @Override
    public void beforeAll(ExtensionContext context) {
        keycloak.start();

        var realm = keycloak.getKeycloakAdminClient().realm("master");
        var clientRepresentation = new ClientRepresentation();
        clientRepresentation.setId(clientId);
        clientRepresentation.setPublicClient(false);
        clientRepresentation.setServiceAccountsEnabled(true);
        clientRepresentation.setClientId("management");
        clientRepresentation.setProtocol("openid-connect");
        clientRepresentation.setAttributes(Map.of(
                "access.token.signed.response.alg", "RS256",
                "use.jwks.url", "false",
                "jwt.credential.client.auth.only", "true"
        ));
        var clients = realm.clients();
        clients.create(clientRepresentation);

        clientSecret = clients.get(clientId).generateNewSecret();
    }

    @Override
    public void afterAll(ExtensionContext context) {
        keycloak.stop();
    }

    public String jwksUrl() {
        return baseUrl() + "/certs";
    }

    public String issueToken() {
        return given()
                .param("grant_type", "client_credentials")
                .param("client_id", "management")
                .param("client_secret", clientSecret.getValue())
                .post(baseUrl() + "/token")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract().body().path("access_token");
    }

    private String baseUrl() {
        return "http://localhost:%s/realms/master/protocol/openid-connect".formatted(keycloak.getHttpPort());
    }

    public String audience() {
        return "account"; // this is the default keycloak audience
    }
}
