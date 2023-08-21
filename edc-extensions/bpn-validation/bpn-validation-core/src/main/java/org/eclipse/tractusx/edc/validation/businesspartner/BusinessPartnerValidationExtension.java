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

package org.eclipse.tractusx.edc.validation.businesspartner;

import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.engine.spi.RuleBindingRegistry;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.tractusx.edc.validation.businesspartner.functions.BusinessPartnerGroupFunction;
import org.eclipse.tractusx.edc.validation.businesspartner.spi.BusinessPartnerStore;

import static org.eclipse.edc.connector.contract.spi.offer.ContractDefinitionResolver.CATALOGING_SCOPE;
import static org.eclipse.edc.connector.contract.spi.validation.ContractValidationService.NEGOTIATION_SCOPE;
import static org.eclipse.edc.connector.contract.spi.validation.ContractValidationService.TRANSFER_SCOPE;
import static org.eclipse.edc.policy.model.OdrlNamespace.ODRL_SCHEMA;

/**
 * Registers a {@link org.eclipse.tractusx.edc.validation.businesspartner.functions.BusinessPartnerGroupFunction} for the following scopes:
 * <ul>
 *     <li>{@link org.eclipse.edc.connector.contract.spi.offer.ContractDefinitionResolver#CATALOGING_SCOPE}</li>
 *     <li>{@link org.eclipse.edc.connector.contract.spi.validation.ContractValidationService#NEGOTIATION_SCOPE}</li>
 *     <li>{@link org.eclipse.edc.connector.contract.spi.validation.ContractValidationService#TRANSFER_SCOPE}</li>
 * </ul>
 * The rule to which the function is bound is {@link BusinessPartnerGroupFunction#BUSINESS_PARTNER_CONSTRAINT_KEY}. That means, that policies that are bound to these scopes look
 * like this:
 * <pre>
 * {
 *     "constraint": {
 *         "leftOperand": "https://w3id.org/tractusx/v0.0.1/ns/BusinessPartnerGroup",
 *         "operator": "isAnyOf",
 *         "rightOperand": ["gold_customer","platin_partner"]
 *     }
 * }
 * </pre>
 * <p>
 * Note that the {@link BusinessPartnerGroupFunction} is an {@link org.eclipse.edc.policy.engine.spi.AtomicConstraintFunction}, thus it is registered with the {@link PolicyEngine}  for the {@link Permission} class.
 */
@Extension(value = "Registers a function to evaluate whether a BPN number is covered by a certain policy or not", categories = { "policy", "contract" })
public class BusinessPartnerValidationExtension implements ServiceExtension {

    private static final String USE = "USE";
    @Inject
    private RuleBindingRegistry ruleBindingRegistry;
    @Inject
    private PolicyEngine policyEngine;
    @Inject
    private BusinessPartnerStore store;

    @Override
    public void initialize(ServiceExtensionContext context) {
        var function = new BusinessPartnerGroupFunction(store);

        bindToScope(function, TRANSFER_SCOPE);
        bindToScope(function, NEGOTIATION_SCOPE);
        bindToScope(function, CATALOGING_SCOPE);
    }

    private void bindToScope(BusinessPartnerGroupFunction function, String scope) {
        ruleBindingRegistry.bind(USE, scope);
        ruleBindingRegistry.bind(ODRL_SCHEMA + "use", scope);
        ruleBindingRegistry.bind(BusinessPartnerGroupFunction.BUSINESS_PARTNER_CONSTRAINT_KEY, scope);

        policyEngine.registerFunction(scope, Permission.class, BusinessPartnerGroupFunction.BUSINESS_PARTNER_CONSTRAINT_KEY, function);
    }
}
