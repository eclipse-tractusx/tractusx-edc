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

package org.eclipse.tractusx.edc.api.edr.v2;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.connector.controlplane.services.spi.contractnegotiation.ContractNegotiationService;
import org.eclipse.edc.edr.spi.store.EndpointDataReferenceStore;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.tractusx.edc.api.edr.BaseEdrCacheApiController;
import org.eclipse.tractusx.edc.edr.spi.service.EdrService;

import static org.eclipse.edc.api.ApiWarnings.deprecationWarning;

@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@Path("/v2/edrs")
@Deprecated(since = "0.8.0")
public class EdrCacheApiV2Controller extends BaseEdrCacheApiController implements EdrCacheApiV2 {

    public EdrCacheApiV2Controller(EndpointDataReferenceStore edrStore,
                                   TypeTransformerRegistry transformerRegistry,
                                   JsonObjectValidatorRegistry validator,
                                   Monitor monitor,
                                   EdrService edrService, ContractNegotiationService contractNegotiationService) {
        super(edrStore, transformerRegistry, validator, monitor, edrService, contractNegotiationService);
    }

    @POST
    @Override
    public JsonObject initiateEdrNegotiationV2(JsonObject requestObject) {
        monitor.warning(deprecationWarning("/v2", "/v3"));
        return super.initiateEdrNegotiation(requestObject);
    }

    @POST
    @Path("/request")
    @Override
    public JsonArray requestEdrEntriesV2(JsonObject querySpecJson) {
        monitor.warning(deprecationWarning("/v2", "/v3"));
        return requestEdrEntries(querySpecJson);
    }

    @GET
    @Path("{transferProcessId}/dataaddress")
    @Override
    public JsonObject getEdrEntryDataAddressV2(@PathParam("transferProcessId") String transferProcessId, @DefaultValue("true") @QueryParam("auto_refresh") boolean autoRefresh) {
        monitor.warning(deprecationWarning("/v2", "/v3"));
        return super.getEdrEntryDataAddress(transferProcessId, autoRefresh);
    }

    @DELETE
    @Path("{transferProcessId}")
    @Override
    public void removeEdrEntryV2(@PathParam("transferProcessId") String transferProcessId) {
        monitor.warning(deprecationWarning("/v2", "/v3"));
        super.removeEdrEntry(transferProcessId);
    }

    @POST
    @Path("{transferProcessId}/refresh")
    @Override
    public JsonObject refreshEdrV2(@PathParam("transferProcessId") String transferProcessId) {
        monitor.warning(deprecationWarning("/v2", "/v3"));
        return super.refreshEdr(transferProcessId);
    }

}
