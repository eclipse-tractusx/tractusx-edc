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

import org.eclipse.edc.sql.testfixtures.PostgresqlLocalInstance;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

import static org.eclipse.edc.junit.testfixtures.TestUtils.getFreePort;

public class TestRuntimeConfiguration {


    public static final String BPN_SUFFIX = "-BPN";
    public static final String SOKRATES_NAME = "SOKRATES";
    public static final String SOKRATES_BPN = SOKRATES_NAME + BPN_SUFFIX;
    public static final String PLATO_NAME = "PLATO";
    public static final String PLATO_BPN = PLATO_NAME + BPN_SUFFIX;
    public static final Integer PLATO_PROXIED_AAS_BACKEND_PORT = getFreePort();

    public static final String PROXIED_PATH = "/events";

    public static final int MIW_PLATO_PORT = getFreePort();

    public static final int MIW_SOKRATES_PORT = getFreePort();

    public static final int OAUTH_PORT = getFreePort();

    static final String DSP_PATH = "/api/v1/dsp";
    static final int PLATO_CONNECTOR_PORT = getFreePort();
    static final int PLATO_MANAGEMENT_PORT = getFreePort();
    static final String PLATO_CONNECTOR_PATH = "/api";
    static final String PLATO_MANAGEMENT_PATH = "/api/v1/management";
    static final int PLATO_DSP_API_PORT = getFreePort();
    public static final String PLATO_DSP_CALLBACK = "http://localhost:" + PLATO_DSP_API_PORT + DSP_PATH;
    static final int SOKRATES_CONNECTOR_PORT = getFreePort();
    static final int SOKRATES_MANAGEMENT_PORT = getFreePort();
    static final String SOKRATES_CONNECTOR_PATH = "/api";
    static final String SOKRATES_MANAGEMENT_PATH = "/api/v1/management";
    static final int SOKRATES_DSP_API_PORT = getFreePort();
    public static final String SOKRATES_DSP_CALLBACK = "http://localhost:" + SOKRATES_DSP_API_PORT + DSP_PATH;
    static final String SOKRATES_PUBLIC_API_PORT = String.valueOf(getFreePort());
    static final String PLATO_PUBLIC_API_PORT = String.valueOf(getFreePort());
    static final String PLATO_DATAPLANE_CONTROL_PORT = String.valueOf(getFreePort());
    static final String PLATO_DATAPLANE_PROXY_PORT = String.valueOf(getFreePort());
    static final String SOKRATES_DATAPLANE_CONTROL_PORT = String.valueOf(getFreePort());
    static final String SOKRATES_DATAPLANE_PROXY_PORT = String.valueOf(getFreePort());
    static final String DB_SCHEMA_NAME = "testschema";
    static final String MIW_SOKRATES_URL = "http://localhost:" + MIW_SOKRATES_PORT;

    static final String MIW_PLATO_URL = "http://localhost:" + MIW_PLATO_PORT;

    static final String OAUTH_TOKEN_URL = "http://localhost:" + OAUTH_PORT;

    public static Map<String, String> sokratesPostgresqlConfiguration() {
        var baseConfiguration = sokratesConfiguration();
        var postgresConfiguration = postgresqlConfiguration(SOKRATES_NAME.toLowerCase());
        baseConfiguration.putAll(postgresConfiguration);
        return baseConfiguration;
    }

    public static Map<String, String> platoPostgresqlConfiguration() {
        var baseConfiguration = platoConfiguration();
        var postgresConfiguration = postgresqlConfiguration(PLATO_NAME.toLowerCase());
        baseConfiguration.putAll(postgresConfiguration);
        return baseConfiguration;
    }

    public static Map<String, String> postgresqlConfiguration(String name) {
        var jdbcUrl = jdbcUrl(name);
        return new HashMap<>() {
            {
                put("edc.datasource.asset.name", "asset");
                put("edc.datasource.asset.url", jdbcUrl);
                put("edc.datasource.asset.user", PostgresqlLocalInstance.USER);
                put("edc.datasource.asset.password", PostgresqlLocalInstance.PASSWORD);
                put("edc.datasource.contractdefinition.name", "contractdefinition");
                put("edc.datasource.contractdefinition.url", jdbcUrl);
                put("edc.datasource.contractdefinition.user", PostgresqlLocalInstance.USER);
                put("edc.datasource.contractdefinition.password", PostgresqlLocalInstance.PASSWORD);
                put("edc.datasource.contractnegotiation.name", "contractnegotiation");
                put("edc.datasource.contractnegotiation.url", jdbcUrl);
                put("edc.datasource.contractnegotiation.user", PostgresqlLocalInstance.USER);
                put("edc.datasource.contractnegotiation.password", PostgresqlLocalInstance.PASSWORD);
                put("edc.datasource.policy.name", "policy");
                put("edc.datasource.policy.url", jdbcUrl);
                put("edc.datasource.policy.user", PostgresqlLocalInstance.USER);
                put("edc.datasource.policy.password", PostgresqlLocalInstance.PASSWORD);
                put("edc.datasource.transferprocess.name", "transferprocess");
                put("edc.datasource.transferprocess.url", jdbcUrl);
                put("edc.datasource.transferprocess.user", PostgresqlLocalInstance.USER);
                put("edc.datasource.transferprocess.password", PostgresqlLocalInstance.PASSWORD);
                put("edc.datasource.edr.name", "edr");
                put("edc.datasource.edr.url", jdbcUrl);
                put("edc.datasource.edr.user", PostgresqlLocalInstance.USER);
                put("edc.datasource.edr.password", PostgresqlLocalInstance.PASSWORD);
                // use non-default schema name to test usage of non-default schema
                put("org.eclipse.tractusx.edc.postgresql.migration.schema", DB_SCHEMA_NAME);
            }
        };
    }

    public static Map<String, String> sokratesSsiConfiguration() {
        var ssiConfiguration = new HashMap<String, String>() {
            {
                put("tx.ssi.miw.url", MIW_SOKRATES_URL);
                put("tx.ssi.oauth.token.url", OAUTH_TOKEN_URL);
                put("tx.ssi.oauth.client.id", "client_id");
                put("tx.ssi.oauth.client.secret.alias", "client_secret_alias");
                put("tx.ssi.miw.authority.id", "authorityId");
                put("tx.ssi.miw.authority.issuer", "did:web:a016-203-129-213-99.ngrok-free.app:BPNL000000000000");
                put("tx.vault.seed.secrets", "client_secret_alias:client_secret");
                put("tx.ssi.endpoint.audience", SOKRATES_DSP_CALLBACK);
            }
        };
        var baseConfiguration = sokratesConfiguration();
        ssiConfiguration.putAll(baseConfiguration);
        return ssiConfiguration;
    }

    public static Map<String, String> sokratesConfiguration() {
        return new HashMap<>() {
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
                put("tx.dpf.consumer.proxy.port", SOKRATES_DATAPLANE_PROXY_PORT);
                put("edc.dataplane.token.validation.endpoint", "http://localhost:" + SOKRATES_DATAPLANE_CONTROL_PORT + "/api/dataplane/control/token");
                put("edc.dataplane.selector.httpplane.url", "http://localhost:" + SOKRATES_DATAPLANE_CONTROL_PORT + "/api/dataplane/control");
                put("edc.dataplane.selector.httpplane.sourcetypes", "HttpData");
                put("edc.dataplane.selector.httpplane.destinationtypes", "HttpProxy");
                put("edc.dataplane.selector.httpplane.properties", "{\"publicApiUrl\":\"http://localhost:" + SOKRATES_PUBLIC_API_PORT + "/api/public\"}");
                put("edc.receiver.http.dynamic.endpoint", "http://localhost:" + SOKRATES_CONNECTOR_PORT + "/api/consumer/datareference");
                put("tractusx.businesspartnervalidation.log.agreement.validation", "true");
                put("edc.agent.identity.key", "BusinessPartnerNumber");
            }
        };
    }

    public static Map<String, String> platoConfiguration() {
        return new HashMap<>() {
            {
                put("edc.connector.name", "plato");
                put("edc.participant.id", PLATO_BPN);
                put("web.http.port", String.valueOf(PLATO_CONNECTOR_PORT));
                put("web.http.path", PLATO_CONNECTOR_PATH);
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
                put("tx.dpf.consumer.proxy.port", PLATO_DATAPLANE_PROXY_PORT);
                put("edc.dataplane.token.validation.endpoint", "http://localhost:" + PLATO_DATAPLANE_CONTROL_PORT + "/api/dataplane/control/token");
                put("edc.dataplane.selector.httpplane.url", "http://localhost:" + PLATO_DATAPLANE_CONTROL_PORT + "/api/dataplane/control");
                put("edc.dataplane.selector.httpplane.sourcetypes", "HttpData");
                put("edc.dataplane.selector.httpplane.destinationtypes", "HttpProxy");
                put("edc.dataplane.selector.httpplane.properties", "{\"publicApiUrl\":\"http://localhost:" + PLATO_PUBLIC_API_PORT + "/api/public\"}");
                put("tractusx.businesspartnervalidation.log.agreement.validation", "true");
                put("edc.agent.identity.key", "BusinessPartnerNumber");
                put("tx.dpf.proxy.gateway.aas.proxied.path", "http://localhost:" + PLATO_PROXIED_AAS_BACKEND_PORT + PROXIED_PATH);
                put("tx.dpf.proxy.gateway.aas.authorization.type", "none");
            }
        };
    }

    public static Map<String, String> platoSsiConfiguration() {
        var ssiConfiguration = new HashMap<String, String>() {
            {
                put("tx.ssi.miw.url", MIW_PLATO_URL);
                put("tx.ssi.oauth.token.url", OAUTH_TOKEN_URL);
                put("tx.ssi.oauth.client.id", "client_id");
                put("tx.ssi.oauth.client.secret.alias", "client_secret_alias");
                put("tx.ssi.miw.authority.id", "authorityId");
                put("tx.ssi.miw.authority.issuer", "did:web:a016-203-129-213-99.ngrok-free.app:BPNL000000000000");
                put("tx.vault.seed.secrets", "client_secret_alias:client_secret");
                put("tx.ssi.endpoint.audience", PLATO_DSP_CALLBACK);
            }
        };
        var baseConfiguration = platoConfiguration();
        ssiConfiguration.putAll(baseConfiguration);
        return ssiConfiguration;
    }

    @NotNull
    public static String jdbcUrl(String name) {
        return PostgresqlLocalInstance.JDBC_URL_PREFIX + name + "?currentSchema=" + DB_SCHEMA_NAME;
    }
}
