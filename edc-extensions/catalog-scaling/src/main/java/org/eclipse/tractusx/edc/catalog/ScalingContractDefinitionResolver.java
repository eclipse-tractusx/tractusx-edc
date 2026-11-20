/*
 * Copyright (c) 2024 Cofinity-X
 * Copyright (c) 2026 Volkswagen AG
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

package org.eclipse.tractusx.edc.catalog;

import org.eclipse.edc.connector.controlplane.catalog.spi.policy.CatalogPolicyContext;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.result.Result;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.eclipse.edc.participantcontext.spi.types.ParticipantResource.queryByParticipantContextId;

//NOTICE: the following file has been used as a baseline: https://github.com/eclipse-edc/Connector/blob/v0.17.0/core/control-plane/control-plane-catalog/src/main/java/org/eclipse/edc/connector/controlplane/catalog/ContractDefinitionResolverImpl.java
public class ScalingContractDefinitionResolver {

    private final PolicyEngine policyEngine;
    private final PolicyDefinitionStore policyStore;
    private final ContractDefinitionStore definitionStore;

    public ScalingContractDefinitionResolver(ContractDefinitionStore contractDefinitionStore,
                                             PolicyEngine policyEngine,
                                             PolicyDefinitionStore policyStore) {
        this.definitionStore = contractDefinitionStore;
        this.policyEngine = policyEngine;
        this.policyStore = policyStore;
    }

    /**
     * Resolves authorized contract definitions for a given participant.
     * <p>
     * Generally same implementation as upstream, but returns a map of authorized contract
     * definitions by ID, instead of a list of contract definitions with cached access policies.
     *
     * @param participantContext participant context id
     * @param agent the requesting participant agent
     * @return all authorized contract definitions by id for the given participant
     */
    public Map<String, ContractDefinition> resolveFor(ParticipantContext participantContext, ParticipantAgent agent) {
        var policies = new HashMap<String, Policy>();
        var query = queryByParticipantContextId(participantContext.getParticipantContextId()).limit(Integer.MAX_VALUE).build();

        try (var definitions = definitionStore.findAll(query)) {
            return definitions
                    .filter(definition -> {
                        var accessResult = Optional.of(definition.getAccessPolicyId())
                                .map(policyId -> policies.computeIfAbsent(policyId,
                                        key -> Optional.ofNullable(policyStore.findById(key))
                                                .map(PolicyDefinition::getPolicy)
                                                .orElse(null))
                                )
                                .map(policy -> policyEngine.evaluate(policy, new CatalogPolicyContext(agent)))
                                .orElse(Result.failure(format("Policy %s not found", definition.getAccessPolicyId())));

                        return accessResult.succeeded();
                    })
                    .collect(Collectors.toMap(ContractDefinition::getId, Function.identity(),
                            (first, second) -> first, LinkedHashMap::new));
        }
    }
}
