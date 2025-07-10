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

package org.eclipse.tractusx.edc.api.bpn.v1;

import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.tractusx.edc.api.bpn.BaseBusinessPartnerGroupApiController;
import org.eclipse.tractusx.edc.validation.businesspartner.spi.observe.BusinessPartnerObservable;
import org.eclipse.tractusx.edc.validation.businesspartner.spi.store.BusinessPartnerStore;

import static org.eclipse.edc.api.ApiWarnings.deprecationWarning;


@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path("/business-partner-groups")
@Deprecated(since = "0.8.0")
public class BusinessPartnerGroupApiV1Controller extends BaseBusinessPartnerGroupApiController implements BusinessPartnerGroupApiV1 {

    private final Monitor monitor;

    public BusinessPartnerGroupApiV1Controller(BusinessPartnerStore businessPartnerService,
                                               BusinessPartnerObservable businessPartnerObservable, Monitor monitor) {
        super(businessPartnerService, businessPartnerObservable);
        this.monitor = monitor;
    }

    @GET
    @Path("/{bpn}")
    @Override
    public JsonObject resolveV1(@PathParam("bpn") String bpn) {
        monitor.warning(deprecationWarning("/v1", "/v3"));
        return super.resolve(bpn);
    }

    @DELETE
    @Path("/{bpn}")
    @Override
    public void deleteEntryV1(@PathParam("bpn") String bpn) {
        monitor.warning(deprecationWarning("/v1", "/v3"));
        super.deleteEntry(bpn);
    }

    @PUT
    @Override
    public void updateEntryV1(@RequestBody JsonObject object) {
        monitor.warning(deprecationWarning("/v1", "/v3"));
        super.updateEntry(object);
    }

    @POST
    @Override
    public void createEntryV1(@RequestBody JsonObject object) {
        monitor.warning(deprecationWarning("/v1", "/v3"));
        super.createEntry(object);
    }

}
