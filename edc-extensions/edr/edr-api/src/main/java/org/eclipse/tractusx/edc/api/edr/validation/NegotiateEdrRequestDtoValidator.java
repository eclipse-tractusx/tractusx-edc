/********************************************************************************
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.api.edr.validation;

import jakarta.json.JsonObject;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.validator.jsonobject.JsonLdPath;
import org.eclipse.edc.validator.jsonobject.JsonObjectValidator;
import org.eclipse.edc.validator.jsonobject.validators.MandatoryObject;
import org.eclipse.edc.validator.jsonobject.validators.MandatoryValue;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;

import static java.lang.String.format;
import static org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractOfferDescription.ASSET_ID;
import static org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractOfferDescription.OFFER_ID;
import static org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractOfferDescription.POLICY;
import static org.eclipse.tractusx.edc.api.edr.dto.NegotiateEdrRequestDto.EDR_REQUEST_DTO_COUNTERPARTY_ADDRESS;
import static org.eclipse.tractusx.edc.api.edr.dto.NegotiateEdrRequestDto.EDR_REQUEST_DTO_OFFER;
import static org.eclipse.tractusx.edc.api.edr.dto.NegotiateEdrRequestDto.EDR_REQUEST_DTO_PROTOCOL;
import static org.eclipse.tractusx.edc.api.edr.dto.NegotiateEdrRequestDto.EDR_REQUEST_DTO_TYPE;


public class NegotiateEdrRequestDtoValidator {

    private NegotiateEdrRequestDtoValidator() {
    }

    public static Validator<JsonObject> instance(Monitor monitor) {
        return JsonObjectValidator.newValidator()
                .verify(EDR_REQUEST_DTO_COUNTERPARTY_ADDRESS, MandatoryValue::new)
                .verify(EDR_REQUEST_DTO_PROTOCOL, MandatoryValue::new)
                .verify(path -> new MandatoryOfferOrPolicy(path, monitor))
                .build();
    }

    private record MandatoryOfferOrPolicy(JsonLdPath path, Monitor monitor) implements Validator<JsonObject> {
        @Override
        public ValidationResult validate(JsonObject input) {
            if (input.containsKey(EDR_REQUEST_DTO_OFFER)) {
                monitor.warning(format("The attribute %s has been deprecated in type %s, please use %s",
                        EDR_REQUEST_DTO_OFFER, EDR_REQUEST_DTO_TYPE, POLICY));
                return new OfferValidator(path.append(EDR_REQUEST_DTO_OFFER)).validate(input);
            }
            return new EdrPolicyValidator(path).validate(input);
        }
    }

    private record OfferValidator(JsonLdPath path) implements Validator<JsonObject> {
        @Override
        public ValidationResult validate(JsonObject input) {
            return JsonObjectValidator.newValidator()
                    .verifyObject(EDR_REQUEST_DTO_OFFER, v -> v
                            .verify(OFFER_ID, MandatoryValue::new)
                            .verify(ASSET_ID, MandatoryValue::new)
                            .verify(POLICY, MandatoryObject::new)
                    ).build().validate(input);
        }
    }

    private record EdrPolicyValidator(JsonLdPath path) implements Validator<JsonObject> {
        @Override
        public ValidationResult validate(JsonObject input) {
            return JsonObjectValidator.newValidator()
                    .verify(POLICY, MandatoryObject::new).build().validate(input);
        }
    }
}
