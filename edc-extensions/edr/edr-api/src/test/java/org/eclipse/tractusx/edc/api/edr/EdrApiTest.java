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

package org.eclipse.tractusx.edc.api.edr;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonObject;
import org.eclipse.edc.api.transformer.JsonObjectToCallbackAddressTransformer;
import org.eclipse.edc.connector.api.management.contractnegotiation.transform.JsonObjectToContractOfferDescriptionTransformer;
import org.eclipse.edc.connector.api.management.contractnegotiation.transform.JsonObjectToContractRequestTransformer;
import org.eclipse.edc.core.transform.TypeTransformerRegistryImpl;
import org.eclipse.edc.core.transform.transformer.odrl.OdrlTransformersFactory;
import org.eclipse.edc.jsonld.JsonLdExtension;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.junit.assertions.AbstractResultAssert;
import org.eclipse.edc.spi.agent.ParticipantIdMapper;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.tractusx.edc.api.edr.dto.NegotiateEdrRequestDto;
import org.eclipse.tractusx.edc.api.edr.transform.JsonObjectToNegotiateEdrRequestDtoTransformer;
import org.eclipse.tractusx.edc.api.edr.validation.NegotiateEdrRequestDtoValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.eclipse.edc.junit.extensions.TestServiceExtensionContext.testServiceExtensionContext;
import static org.eclipse.tractusx.edc.api.edr.schema.EdrSchema.EndpointDataReferenceEntrySchema.ENDPOINT_DATA_REFERENCE_ENTRY_EXAMPLE;
import static org.eclipse.tractusx.edc.api.edr.schema.EdrSchema.NegotiateEdrRequestSchema.NEGOTIATE_EDR_REQUEST_EXAMPLE;
import static org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntry.EDR_ENTRY_AGREEMENT_ID;
import static org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntry.EDR_ENTRY_ASSET_ID;
import static org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntry.EDR_ENTRY_EXPIRATION_DATE;
import static org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntry.EDR_ENTRY_PROVIDER_ID;
import static org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntry.EDR_ENTRY_STATE;
import static org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntry.EDR_ENTRY_TRANSFER_PROCESS_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EdrApiTest {

    private final ObjectMapper objectMapper = JacksonJsonLd.createObjectMapper();
    private final JsonLd jsonLd = new JsonLdExtension().createJsonLdService(testServiceExtensionContext());

    private final TypeTransformerRegistry transformer = new TypeTransformerRegistryImpl();


    @BeforeEach
    void setUp() {
        transformer.register(new JsonObjectToContractRequestTransformer());
        transformer.register(new JsonObjectToContractOfferDescriptionTransformer());
        transformer.register(new JsonObjectToCallbackAddressTransformer());
        transformer.register(new JsonObjectToNegotiateEdrRequestDtoTransformer());
        var mapper = mock(ParticipantIdMapper.class);

        when(mapper.fromIri(any())).thenAnswer(a -> a.getArgument(0));
        when(mapper.toIri(any())).thenAnswer(a -> a.getArgument(0));
        OdrlTransformersFactory.jsonObjectToOdrlTransformers(mapper).forEach(transformer::register);
    }

    @Test
    void edrRequestExample() throws JsonProcessingException {
        var validator = NegotiateEdrRequestDtoValidator.instance();

        var jsonObject = objectMapper.readValue(NEGOTIATE_EDR_REQUEST_EXAMPLE, JsonObject.class);
        assertThat(jsonObject).isNotNull();

        var expanded = jsonLd.expand(jsonObject);
        AbstractResultAssert.assertThat(expanded).isSucceeded()
                .satisfies(exp -> AbstractResultAssert.assertThat(validator.validate(exp)).isSucceeded())
                .extracting(e -> transformer.transform(e, NegotiateEdrRequestDto.class))
                .satisfies(transformResult -> AbstractResultAssert.assertThat(transformResult).isSucceeded()
                        .satisfies(transformed -> {
                            assertThat(transformed.getOffer()).isNotNull();
                            assertThat(transformed.getCallbackAddresses()).asList().hasSize(1);
                            assertThat(transformed.getProviderId()).isNotBlank();
                        }));
    }

    @Test
    void edrEntryExample() throws JsonProcessingException {

        var jsonObject = objectMapper.readValue(ENDPOINT_DATA_REFERENCE_ENTRY_EXAMPLE, JsonObject.class);
        assertThat(jsonObject).isNotNull();

        var expanded = jsonLd.expand(jsonObject);

        AbstractResultAssert.assertThat(expanded).isSucceeded().satisfies(content -> {

            assertThat(first(content, EDR_ENTRY_STATE).getJsonString(VALUE).getString())
                    .isEqualTo(jsonObject.getString("tx:edrState"));

            assertThat(first(content, EDR_ENTRY_ASSET_ID).getJsonString(VALUE).getString())
                    .isEqualTo(jsonObject.getString("assetId"));

            assertThat(first(content, EDR_ENTRY_AGREEMENT_ID).getJsonString(VALUE).getString())
                    .isEqualTo(jsonObject.getString("agreementId"));

            assertThat(first(content, EDR_ENTRY_TRANSFER_PROCESS_ID).getJsonString(VALUE).getString())
                    .isEqualTo(jsonObject.getString("transferProcessId"));

            assertThat(first(content, EDR_ENTRY_PROVIDER_ID).getJsonString(VALUE).getString())
                    .isEqualTo(jsonObject.getString("providerId"));

            assertThat(first(content, EDR_ENTRY_EXPIRATION_DATE).getJsonNumber(VALUE).longValue())
                    .isEqualTo(jsonObject.getJsonNumber("tx:expirationDate").longValue());
        });
    }

    private JsonObject first(JsonObject content, String name) {
        return content.getJsonArray(name).getJsonObject(0);
    }
}
