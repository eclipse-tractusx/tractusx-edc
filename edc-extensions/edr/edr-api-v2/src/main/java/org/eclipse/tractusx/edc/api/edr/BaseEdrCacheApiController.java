/*
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
 */

package org.eclipse.tractusx.edc.api.edr;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.eclipse.edc.api.model.IdResponse;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequest;
import org.eclipse.edc.connector.controlplane.services.spi.contractnegotiation.ContractNegotiationService;
import org.eclipse.edc.edr.spi.store.EndpointDataReferenceStore;
import org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.ValidationFailureException;
import org.eclipse.tractusx.edc.edr.spi.service.EdrService;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static jakarta.json.stream.JsonCollectors.toJsonArray;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequest.CONTRACT_REQUEST_TYPE;
import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_TYPE;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;
import static org.eclipse.tractusx.edc.edr.spi.types.RefreshMode.AUTO_REFRESH;
import static org.eclipse.tractusx.edc.edr.spi.types.RefreshMode.FORCE_REFRESH;
import static org.eclipse.tractusx.edc.edr.spi.types.RefreshMode.NO_REFRESH;

public class BaseEdrCacheApiController {

    public static final String LOCAL_ADAPTER_URI = "local://adapter";
    public static final Set<String> LOCAL_EVENTS = Set.of("contract.negotiation", "transfer.process");
    public static final CallbackAddress LOCAL_CALLBACK = CallbackAddress.Builder.newInstance()
            .transactional(true)
            .uri(LOCAL_ADAPTER_URI)
            .events(LOCAL_EVENTS)
            .build();
    private final EndpointDataReferenceStore edrStore;
    private final TypeTransformerRegistry transformerRegistry;
    private final JsonObjectValidatorRegistry validator;
    protected final Monitor monitor;
    private final EdrService edrService;

    private final ContractNegotiationService contractNegotiationService;

    public BaseEdrCacheApiController(EndpointDataReferenceStore edrStore,
                                     TypeTransformerRegistry transformerRegistry,
                                     JsonObjectValidatorRegistry validator,
                                     Monitor monitor,
                                     EdrService edrService, ContractNegotiationService contractNegotiationService) {
        this.edrStore = edrStore;
        this.transformerRegistry = transformerRegistry;
        this.validator = validator;
        this.monitor = monitor;
        this.edrService = edrService;
        this.contractNegotiationService = contractNegotiationService;
    }

    public JsonObject initiateEdrNegotiation(JsonObject requestObject) {

        validator.validate(CONTRACT_REQUEST_TYPE, requestObject)
                .orElseThrow(ValidationFailureException::new);

        var contractRequest = transformerRegistry.transform(requestObject, ContractRequest.class)
                .orElseThrow(InvalidRequestException::new);

        var contractNegotiation = contractNegotiationService.initiateNegotiation(enrichContractRequest(contractRequest));

        var idResponse = IdResponse.Builder.newInstance()
                .id(contractNegotiation.getId())
                .createdAt(contractNegotiation.getCreatedAt())
                .build();

        return transformerRegistry.transform(idResponse, JsonObject.class)
                .orElseThrow(f -> new EdcException("Error creating response body: " + f.getFailureDetail()));
    }

    public JsonArray requestEdrEntries(JsonObject querySpecJson) {
        QuerySpec querySpec;
        if (querySpecJson == null) {
            querySpec = QuerySpec.Builder.newInstance().build();
        } else {
            validator.validate(EDC_QUERY_SPEC_TYPE, querySpecJson).orElseThrow(ValidationFailureException::new);

            querySpec = transformerRegistry.transform(querySpecJson, QuerySpec.class)
                    .orElseThrow(InvalidRequestException::new);
        }

        return edrStore.query(querySpec)
                .flatMap(ServiceResult::from)
                .orElseThrow(exceptionMapper(QuerySpec.class, null)).stream()
                .map(it -> transformerRegistry.transform(it, JsonObject.class))
                .peek(r -> r.onFailure(f -> monitor.warning(f.getFailureDetail())))
                .filter(Result::succeeded)
                .map(Result::getContent)
                .collect(toJsonArray());
    }

    public JsonObject getEdrEntryDataAddress(String transferProcessId, boolean autoRefresh) {
        var mode = autoRefresh ? AUTO_REFRESH : NO_REFRESH;
        var dataAddress = edrService.resolveByTransferProcess(transferProcessId, mode)
                .orElseThrow(exceptionMapper(EndpointDataReferenceEntry.class, transferProcessId));

        return transformerRegistry.transform(dataAddress, JsonObject.class)
                .orElseThrow(f -> new EdcException(f.getFailureDetail()));
    }

    public void removeEdrEntry(String transferProcessId) {
        edrStore.delete(transferProcessId)
                .flatMap(ServiceResult::from)
                .orElseThrow(exceptionMapper(EndpointDataReferenceEntry.class, transferProcessId));
    }

    public JsonObject refreshEdr(String transferProcessId) {
        var updatedEdr = edrService.resolveByTransferProcess(transferProcessId, FORCE_REFRESH)
                .orElseThrow(exceptionMapper(EndpointDataReferenceEntry.class, transferProcessId));

        return transformerRegistry.transform(updatedEdr, JsonObject.class)
                .orElseThrow(f -> new EdcException(f.getFailureDetail()));
    }

    private ContractRequest enrichContractRequest(ContractRequest request) {
        var callbacks = Stream.concat(request.getCallbackAddresses().stream(), Stream.of(LOCAL_CALLBACK)).collect(Collectors.toList());

        return ContractRequest.Builder.newInstance()
                .counterPartyAddress(request.getCounterPartyAddress())
                .contractOffer(request.getContractOffer())
                .protocol(request.getProtocol())
                .callbackAddresses(callbacks).build();
    }

}
