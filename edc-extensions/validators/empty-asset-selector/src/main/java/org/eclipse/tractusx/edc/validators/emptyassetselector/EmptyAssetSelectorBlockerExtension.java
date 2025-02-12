/********************************************************************************
 * Copyright (c) 2025 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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
 ********************************************************************************/

package org.eclipse.tractusx.edc.validators.emptyassetselector;

import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;

import static org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition.CONTRACT_DEFINITION_TYPE;

@Extension(value = EmptyAssetSelectorBlockerExtension.NAME)
public class EmptyAssetSelectorBlockerExtension implements ServiceExtension {

    public static final String NAME = "Empty Asset Selector Blocker extension";

    private static final String BLOCKER_DISABLED = "false";

    @Setting(description = "Block contract definitions from being created/updated with an empty asset selector.", defaultValue = BLOCKER_DISABLED, key = "tx.edc.validator.contractdefinitions.block-empty-asset-selector")
    private boolean blockerEnabled;

    @Inject
    JsonObjectValidatorRegistry validatorRegistry;

    @Inject
    CriterionOperatorRegistry criterionOperatorRegistry;

    @Inject
    Monitor monitor;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void prepare() {
        if (blockerEnabled) {
            monitor.info("ContractDefinition validator that blocks empty assetsSelector has been enabled");
            validatorRegistry.register(CONTRACT_DEFINITION_TYPE, EmptyAssetSelectorValidator.instance(criterionOperatorRegistry));
        }
    }

}
