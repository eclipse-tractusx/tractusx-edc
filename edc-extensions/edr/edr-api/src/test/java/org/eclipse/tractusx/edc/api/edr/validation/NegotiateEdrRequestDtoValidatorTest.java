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

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import org.eclipse.edc.validator.spi.ValidationFailure;
import org.eclipse.edc.validator.spi.Validator;
import org.eclipse.edc.validator.spi.Violation;
import org.junit.jupiter.api.Test;

import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractOfferDescription.ASSET_ID;
import static org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractOfferDescription.OFFER_ID;
import static org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractOfferDescription.POLICY;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.tractusx.edc.api.edr.dto.NegotiateEdrRequestDto.EDR_REQUEST_DTO_CONNECTOR_ADDRESS;
import static org.eclipse.tractusx.edc.api.edr.dto.NegotiateEdrRequestDto.EDR_REQUEST_DTO_OFFER;
import static org.eclipse.tractusx.edc.api.edr.dto.NegotiateEdrRequestDto.EDR_REQUEST_DTO_PROTOCOL;
import static org.eclipse.tractusx.edc.api.edr.dto.NegotiateEdrRequestDto.EDR_REQUEST_DTO_PROVIDER_ID;

public class NegotiateEdrRequestDtoValidatorTest {

    private final Validator<JsonObject> validator = NegotiateEdrRequestDtoValidator.instance();

    @Test
    void shouldSuccess_whenObjectIsValid() {
        var input = Json.createObjectBuilder()
                .add(EDR_REQUEST_DTO_CONNECTOR_ADDRESS, value("http://connector-address"))
                .add(EDR_REQUEST_DTO_PROTOCOL, value("protocol"))
                .add(EDR_REQUEST_DTO_PROVIDER_ID, value("connector-id"))
                .add(EDR_REQUEST_DTO_OFFER, createArrayBuilder().add(createObjectBuilder()
                        .add(OFFER_ID, value("offerId"))
                        .add(ASSET_ID, value("offerId"))
                        .add(POLICY, createArrayBuilder().add(createObjectBuilder()))
                ))
                .build();

        var result = validator.validate(input);

        assertThat(result).isSucceeded();
    }

    @Test
    void shouldFail_whenMandatoryPropertiesAreMissing() {
        var input = Json.createObjectBuilder().build();

        var result = validator.validate(input);

        assertThat(result).isFailed().extracting(ValidationFailure::getViolations).asInstanceOf(list(Violation.class))
                .isNotEmpty()
                .anySatisfy(violation -> assertThat(violation.path()).isEqualTo(EDR_REQUEST_DTO_CONNECTOR_ADDRESS))
                .anySatisfy(violation -> assertThat(violation.path()).isEqualTo(EDR_REQUEST_DTO_PROTOCOL))
                .anySatisfy(violation -> assertThat(violation.path()).isEqualTo(EDR_REQUEST_DTO_OFFER));
    }

    @Test
    void shouldFail_whenOfferMandatoryPropertiesAreMissing() {
        var input = Json.createObjectBuilder()
                .add(EDR_REQUEST_DTO_CONNECTOR_ADDRESS, value("http://connector-address"))
                .add(EDR_REQUEST_DTO_PROTOCOL, value("protocol"))
                .add(EDR_REQUEST_DTO_PROVIDER_ID, value("connector-id"))
                .add(EDR_REQUEST_DTO_OFFER, createArrayBuilder().add(createObjectBuilder()))
                .build();

        var result = validator.validate(input);

        assertThat(result).isFailed().extracting(ValidationFailure::getViolations).asInstanceOf(list(Violation.class))
                .isNotEmpty()
                .anySatisfy(violation -> assertThat(violation.path()).isEqualTo(EDR_REQUEST_DTO_OFFER + "/" + OFFER_ID))
                .anySatisfy(violation -> assertThat(violation.path()).isEqualTo(EDR_REQUEST_DTO_OFFER + "/" + ASSET_ID))
                .anySatisfy(violation -> assertThat(violation.path()).isEqualTo(EDR_REQUEST_DTO_OFFER + "/" + POLICY));
    }

    private JsonArrayBuilder value(String value) {
        return createArrayBuilder().add(createObjectBuilder().add(VALUE, value));
    }
}
