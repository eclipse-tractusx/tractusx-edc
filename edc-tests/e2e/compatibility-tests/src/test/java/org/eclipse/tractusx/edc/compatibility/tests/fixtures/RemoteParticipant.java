/*******************************************************************************
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 * Copyright (c) 2025 Cofinity-X GmbH
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
 ******************************************************************************/

package org.eclipse.tractusx.edc.compatibility.tests.fixtures;

import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.tractusx.edc.tests.participant.IatpParticipant;
import org.eclipse.tractusx.edc.tests.runtimes.PostgresExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.eclipse.edc.util.io.Ports.getFreePort;

public class RemoteParticipant extends IatpParticipant {
    private final List<String> datasources = List.of("asset", "contractdefinition",
            "contractnegotiation", "policy", "transferprocess", "bpn",
            "policy-monitor", "edr", "dataplane", "accesstokendata", "dataplaneinstance");

    public Config getConfig(IatpParticipant participant, PostgresExtension postgresql) {
        var postgresqlConfig = postgresql.getConfig(getName());

        Map<String, String>settings =  new HashMap<>() {
            {
                put("edc.participant.id", id);
                put("edc.api.auth.key", MANAGEMENT_API_KEY);
                put("web.http.port", String.valueOf(getFreePort()));
                put("web.http.path", "/api");
                put("web.http.protocol.port", String.valueOf(controlPlaneProtocol.get().getPort()));
                put("web.http.protocol.path", controlPlaneProtocol.get().getPath());
                put("web.http.management.port", String.valueOf(controlPlaneManagement.get().getPort()));
                put("web.http.management.path", controlPlaneManagement.get().getPath());
                put("web.http.control.port", String.valueOf(getFreePort()));
                put("web.http.control.path", "/control");
                put("web.http.catalog.port", String.valueOf(federatedCatalog.get().getPort()));
                put("web.http.catalog.path", federatedCatalog.get().getPath());
                put("web.http.catalog.auth.type", "tokenbased");
                put("web.http.catalog.auth.key", MANAGEMENT_API_KEY);
                put("edc.transfer.send.retry.limit", "1");
                put("edc.transfer.send.retry.base-delay.ms", "100");
                put("edc.dsp.callback.address", controlPlaneProtocol.get().toString());
                putAll(datasourceEnvironmentVariables("default", postgresqlConfig));
                put("edc.iam.sts.oauth.token.url", stsUri.get().toString() + "/token");
                put("edc.iam.sts.oauth.client.id", getDid());
                put("edc.iam.sts.oauth.client.secret.alias", "client_secret_alias");
                put("testing.edc.vaults.1.key", "client_secret_alias");
                put("testing.edc.vaults.1.value", "clientSecret");
                put("testing.edc.vaults.2.key", getPrivateKeyAlias());
                put("testing.edc.vaults.2.value", getPrivateKeyAsString());
                put("testing.edc.vaults.3.key", getFullKeyId());
                put("testing.edc.vaults.3.value", getPublicKeyAsString());
                put("edc.iam.issuer.id", getDid());
                put("edc.iam.did.web.use.https", "false");
                put("testing.edc.bdrs.1.key", participant.getId());
                put("testing.edc.bdrs.1.value", participant.getDid());
                put("edc.iam.trusted-issuer.issuer.id", trustedIssuer);
                put("edc.sql.schema.autocreate", "false");
                put("web.http.public.path", dataPlanePublic.get().getPath());
                put("web.http.public.port", String.valueOf(dataPlanePublic.get().getPort()));
                put("edc.transfer.proxy.token.signer.privatekey.alias", getPrivateKeyAlias());
                put("edc.transfer.proxy.token.verifier.publickey.alias", getFullKeyId());
                putAll(datasourceConfig(postgresqlConfig));
            }
        };

        return ConfigFactory.fromMap(settings);
    }

    private Map<String, String> datasourceConfig(Config postgresqlConfig) {

        var config = new HashMap<String, String>();
        datasources.forEach(ds -> {
            config.put("edc.datasource." + ds + ".name", ds);
            config.putAll(datasourceEnvironmentVariables(ds, postgresqlConfig));
        });
        config.put("org.eclipse.tractusx.edc.postgresql.migration.schema", postgresqlConfig.getString("tx.edc.postgresql.migration.schema"));
        return config;
    }

    private Map<String, String> datasourceEnvironmentVariables(String datasourceName, Config postgresqlConfig) {
        return Map.of(
                "edc.datasource." + datasourceName + ".url", postgresqlConfig.getString("edc.datasource.default.url"),
                "edc.datasource." + datasourceName + ".user", postgresqlConfig.getString("edc.datasource.default.user"),
                "edc.datasource." + datasourceName + ".password", postgresqlConfig.getString("edc.datasource.default.password")
        );
    }

    public static class Builder extends IatpParticipant.Builder {

        protected Builder() {
            super(new RemoteParticipant());
        }

        public static Builder newInstance() {
            return new Builder();
        }

        @Override
        public Builder name(String name) {
            super.name(name);
            return this;
        }

        @Override
        public Builder id(String id) {
            super.id(id);
            return this;
        }

        @Override
        public Builder stsUri(org.eclipse.edc.junit.utils.LazySupplier<java.net.URI> stsUri) {
            super.stsUri(stsUri);
            return this;
        }

        @Override
        public Builder did(String did) {
            super.did(did);
            return this;
        }

        @Override
        public Builder trustedIssuer(String trustedIssuer) {
            super.trustedIssuer(trustedIssuer);
            return this;
        }

        @Override
        public Builder bpn(String bpn) {
            super.bpn(bpn);
            return this;
        }

        @Override
        public RemoteParticipant build() {
            /*RemoteParticipant remoteParticipant = (RemoteParticipant) this.participant;
            remoteParticipant.enrichManagementRequest = request -> request.header("x-api-key", MANAGEMENT_API_KEY);*/
            return (RemoteParticipant) super.build();
        }
    }
}
