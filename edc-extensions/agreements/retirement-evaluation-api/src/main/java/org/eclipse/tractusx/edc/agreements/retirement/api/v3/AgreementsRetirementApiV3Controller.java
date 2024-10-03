/********************************************************************************
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.agreements.retirement.api.v3;

import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.ValidationFailureException;
import org.eclipse.tractusx.edc.agreements.retirement.spi.store.AgreementsRetirementStore;
import org.eclipse.tractusx.edc.agreements.retirement.spi.types.AgreementsRetirementEntry;

import static jakarta.json.stream.JsonCollectors.toJsonArray;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_TYPE;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v3.1alpha/retireagreements")
public class AgreementsRetirementApiV3Controller implements AgreementsRetirementApiV3 {

    private final AgreementsRetirementStore service;
    private final TypeTransformerRegistry transformerRegistry;
    private final JsonObjectValidatorRegistry validator;
    private final Monitor monitor;


    public AgreementsRetirementApiV3Controller(AgreementsRetirementStore service, TypeTransformerRegistry transformerRegistry, JsonObjectValidatorRegistry validator, Monitor monitor) {
        this.service = service;
        this.transformerRegistry = transformerRegistry;
        this.validator = validator;
        this.monitor = monitor;
    }

    @POST
    @Path("/request")
    @Override
    public JsonArray getAllRetiredV3(@RequestBody JsonObject querySpecJson) {

        QuerySpec querySpec;
        if (querySpecJson == null) {
            querySpec = QuerySpec.max();
        } else {
            validator.validate(EDC_QUERY_SPEC_TYPE, querySpecJson).orElseThrow(ValidationFailureException::new);

            querySpec = transformerRegistry.transform(querySpecJson, QuerySpec.class)
                    .orElseThrow(InvalidRequestException::new);
        }

        return service.findRetiredAgreements(querySpec)
                .flatMap(ServiceResult::from)
                .orElseThrow(exceptionMapper(QuerySpec.class, null)).stream()
                .map(it -> transformerRegistry.transform(it, JsonObject.class))
                .peek(r -> r.onFailure(f -> monitor.warning(f.getFailureDetail())))
                .filter(Result::succeeded)
                .map(Result::getContent)
                .collect(toJsonArray());
    }

    @DELETE
    @Path("/{agreementId}")
    @Override
    public void reactivateRetiredV3(@PathParam("agreementId") String agreementId) {
        service.delete(agreementId)
                .flatMap(ServiceResult::from)
                .orElseThrow(exceptionMapper(AgreementsRetirementEntry.class, agreementId));
    }

    @POST
    @Override
    public void retireAgreementV3(@RequestBody JsonObject entry) {
        // map JsonObject entry to AgreementRetirementEntry
        validator.validate(AgreementsRetirementEntry.AR_ENTRY_TYPE, entry).orElseThrow(ValidationFailureException::new);

        var retirementEntry = transformerRegistry.transform(entry, AgreementsRetirementEntry.class)
                .orElseThrow(InvalidRequestException::new);

        service.save(retirementEntry).flatMap(ServiceResult::from)
                .orElseThrow(exceptionMapper(AgreementsRetirementEntry.class, retirementEntry.getAgreementId()));


    }
}
