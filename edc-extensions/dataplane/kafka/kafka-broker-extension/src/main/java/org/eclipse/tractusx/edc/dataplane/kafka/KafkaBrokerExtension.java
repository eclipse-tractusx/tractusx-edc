/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.edc.dataplane.kafka;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.eclipse.edc.connector.dataplane.spi.edr.EndpointDataReferenceServiceRegistry;
import org.eclipse.edc.connector.dataplane.spi.provision.ProvisionerManager;
import org.eclipse.edc.connector.dataplane.spi.provision.ResourceDefinitionGeneratorManager;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.tractusx.edc.dataplane.kafka.acl.KafkaAclService;
import org.eclipse.tractusx.edc.dataplane.kafka.acl.KafkaAclServiceImpl;
import org.eclipse.tractusx.edc.dataplane.kafka.auth.KafkaOauthService;
import org.eclipse.tractusx.edc.dataplane.kafka.auth.KafkaOauthServiceImpl;
import org.eclipse.tractusx.edc.dataplane.kafka.flow.KafkaEndpointDataReferenceService;
import org.eclipse.tractusx.edc.dataplane.kafka.provision.KafkaDeprovisioner;
import org.eclipse.tractusx.edc.dataplane.kafka.provision.KafkaProvisioner;
import org.eclipse.tractusx.edc.dataplane.kafka.provision.KafkaResourceDefinitionGenerator;

import java.util.Properties;

import static org.eclipse.tractusx.edc.dataplane.kafka.dataaddress.KafkaBrokerDataAddressSchema.KAFKA_TYPE;

/**
 * Kafka streaming data-plane extension.
 * <p>
 * Adds the {@code KafkaBroker-PULL} transfer type to the EDC data plane: on transfer start a fresh OAuth2
 * token is minted and (optionally) broker ACLs are created, and the consumer is handed an EDR pointing
 * directly at the Kafka broker. On suspend/terminate the ACLs and token are revoked.
 * <p>
 * Set {@code edc.dataplane.kafka.acl.enabled=true} to activate Kafka ACL management for immediate
 * broker-level revocation independent of token expiry.
 */
@Extension(value = KafkaBrokerExtension.NAME)
public class KafkaBrokerExtension implements ServiceExtension {

    public static final String NAME = "Kafka stream extension";

    @Setting(description = "Enable Kafka ACL management for immediate broker-level access revocation on transfer termination", defaultValue = "false")
    static final String ACL_ENABLED = "edc.dataplane.kafka.acl.enabled";

    @Setting(description = "Kafka bootstrap servers used by the ACL admin client (required when ACL management is enabled)")
    static final String ACL_BOOTSTRAP_SERVERS = "edc.dataplane.kafka.acl.bootstrap.servers";

    @Setting(description = "Security protocol for the ACL admin client", defaultValue = "PLAINTEXT")
    static final String ACL_SECURITY_PROTOCOL = "edc.dataplane.kafka.acl.security.protocol";

    @Setting(description = "SASL mechanism for the ACL admin client (e.g. OAUTHBEARER, PLAIN)")
    static final String ACL_SASL_MECHANISM = "edc.dataplane.kafka.acl.sasl.mechanism";

    @Setting(description = "SASL JAAS config for the ACL admin client")
    static final String ACL_SASL_JAAS_CONFIG = "edc.dataplane.kafka.acl.sasl.jaas.config";

    @Inject
    private Vault vault;

    @Inject
    private TypeManager typeManager;

    @Inject
    private EdcHttpClient httpClient;

    @Inject
    private ResourceDefinitionGeneratorManager resourceDefinitionGeneratorManager;

    @Inject
    private ProvisionerManager provisionerManager;

    @Inject
    private EndpointDataReferenceServiceRegistry endpointDataReferenceServiceRegistry;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(final ServiceExtensionContext context) {
        var monitor = context.getMonitor();
        KafkaOauthService oauthService = new KafkaOauthServiceImpl(httpClient, typeManager.getMapper());
        KafkaAclService aclService = buildAclService(context);

        resourceDefinitionGeneratorManager.registerProviderGenerator(new KafkaResourceDefinitionGenerator());
        provisionerManager.register(new KafkaProvisioner(vault, oauthService, monitor));
        provisionerManager.register(new KafkaDeprovisioner(vault, oauthService, aclService, monitor));
        endpointDataReferenceServiceRegistry.register(KAFKA_TYPE, new KafkaEndpointDataReferenceService(aclService, monitor, typeManager.getMapper()));
    }

    private KafkaAclService buildAclService(ServiceExtensionContext context) {
        if (!Boolean.parseBoolean(context.getSetting(ACL_ENABLED, "false"))) {
            return null;
        }
        return new KafkaAclServiceImpl(buildAdminProperties(context), context.getMonitor());
    }

    private Properties buildAdminProperties(ServiceExtensionContext context) {
        var props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, context.getSetting(ACL_BOOTSTRAP_SERVERS, ""));
        props.put("security.protocol", context.getSetting(ACL_SECURITY_PROTOCOL, "PLAINTEXT"));

        var saslMechanism = context.getSetting(ACL_SASL_MECHANISM, null);
        if (saslMechanism != null) {
            props.put("sasl.mechanism", saslMechanism);
        }

        var jaasConfig = context.getSetting(ACL_SASL_JAAS_CONFIG, null);
        if (jaasConfig != null) {
            props.put("sasl.jaas.config", jaasConfig);
        }

        return props;
    }
}
