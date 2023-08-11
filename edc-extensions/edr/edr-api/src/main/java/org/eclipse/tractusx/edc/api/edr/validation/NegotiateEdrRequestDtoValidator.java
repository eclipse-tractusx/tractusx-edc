/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.tractusx.edc.api.edr.validation;

import jakarta.json.JsonObject;
import org.eclipse.edc.validator.jsonobject.JsonObjectValidator;
import org.eclipse.edc.validator.jsonobject.validators.MandatoryObject;
import org.eclipse.edc.validator.jsonobject.validators.MandatoryValue;
import org.eclipse.edc.validator.spi.Validator;

import static org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractOfferDescription.ASSET_ID;
import static org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractOfferDescription.OFFER_ID;
import static org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractOfferDescription.POLICY;
import static org.eclipse.tractusx.edc.api.edr.dto.NegotiateEdrRequestDto.EDR_REQUEST_DTO_CONNECTOR_ADDRESS;
import static org.eclipse.tractusx.edc.api.edr.dto.NegotiateEdrRequestDto.EDR_REQUEST_DTO_OFFER;
import static org.eclipse.tractusx.edc.api.edr.dto.NegotiateEdrRequestDto.EDR_REQUEST_DTO_PROTOCOL;


public class NegotiateEdrRequestDtoValidator {

    private NegotiateEdrRequestDtoValidator() {
    }

    public static Validator<JsonObject> instance() {
        return JsonObjectValidator.newValidator()
                .verify(EDR_REQUEST_DTO_CONNECTOR_ADDRESS, MandatoryValue::new)
                .verify(EDR_REQUEST_DTO_PROTOCOL, MandatoryValue::new)
                .verify(EDR_REQUEST_DTO_OFFER, MandatoryObject::new)
                .verifyObject(EDR_REQUEST_DTO_OFFER, v -> v
                        .verify(OFFER_ID, MandatoryValue::new)
                        .verify(ASSET_ID, MandatoryValue::new)
                        .verify(POLICY, MandatoryObject::new)
                )
                .build();
    }
}
