/*
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft
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

package org.eclipse.tractusx.edc.api.edr.v2.validation;

import jakarta.json.JsonObject;
import org.eclipse.edc.validator.jsonobject.JsonObjectValidator;
import org.eclipse.edc.validator.jsonobject.validators.MandatoryObject;
import org.eclipse.edc.validator.jsonobject.validators.MandatoryValue;
import org.eclipse.edc.validator.spi.Validator;
import org.eclipse.tractusx.edc.api.edr.v2.dto.NegotiateEdrRequestDto;

import static org.eclipse.edc.connector.controlplane.api.management.contractnegotiation.model.ContractOfferDescription.OFFER_ID;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequest.POLICY;
import static org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry.ASSET_ID;


public class NegotiateEdrRequestDtoValidator {

    private NegotiateEdrRequestDtoValidator() {
    }

    public static Validator<JsonObject> instance() {
        return JsonObjectValidator.newValidator()
                .verify(NegotiateEdrRequestDto.EDR_REQUEST_DTO_COUNTERPARTY_ADDRESS, MandatoryValue::new)
                .verify(NegotiateEdrRequestDto.EDR_REQUEST_DTO_PROTOCOL, MandatoryValue::new)
                .verify(NegotiateEdrRequestDto.EDR_REQUEST_DTO_OFFER, MandatoryObject::new)
                .verifyObject(NegotiateEdrRequestDto.EDR_REQUEST_DTO_OFFER, v -> v
                        .verify(OFFER_ID, MandatoryValue::new)
                        .verify(ASSET_ID, MandatoryValue::new)
                        .verify(POLICY, MandatoryObject::new)
                )
                .build();
    }
}
