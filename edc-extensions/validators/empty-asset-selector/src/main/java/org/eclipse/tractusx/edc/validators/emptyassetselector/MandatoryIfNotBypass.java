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

import jakarta.json.JsonObject;
import org.eclipse.edc.validator.jsonobject.JsonLdPath;
import org.eclipse.edc.validator.jsonobject.validators.MandatoryArray;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;

import java.util.Optional;

import static org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition.CONTRACT_DEFINITION_PRIVATE_PROPERTIES;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

public class MandatoryIfNotBypass implements Validator<JsonObject> {

    private static final String BYPASS_PROPERTY = EDC_NAMESPACE + "allowEmpty";
    private static final String BYPASS_VALUE = "assetSelector";

    private final JsonLdPath path;
    private final Validator<JsonObject> mandatoryValidator;
    private final JsonLdPath privatePropertiesPath;

    public MandatoryIfNotBypass(JsonLdPath path) {
        this.path = path;
        this.mandatoryValidator = new MandatoryArray(path, 1);
        this.privatePropertiesPath = JsonLdPath.path(CONTRACT_DEFINITION_PRIVATE_PROPERTIES);
    }

    @Override
    public ValidationResult validate(JsonObject input) {
        return Optional.ofNullable(input.getJsonArray(privatePropertiesPath.last()))
                .map(it -> it.getJsonObject(0))
                .map(it -> it.getJsonArray(BYPASS_PROPERTY))
                .map(it -> it.getJsonObject(0))
                .map(it -> it.getString(VALUE))
                .filter(value -> value.equals(BYPASS_VALUE))
                .map(it -> ValidationResult.success())
                .orElse(mandatoryValidator.validate(input));
    }
}
