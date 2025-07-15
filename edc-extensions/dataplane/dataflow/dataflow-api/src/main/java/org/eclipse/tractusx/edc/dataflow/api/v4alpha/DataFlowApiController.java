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

package org.eclipse.tractusx.edc.dataflow.api.v4alpha;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import org.eclipse.edc.connector.dataplane.spi.DataFlow;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.tractusx.edc.spi.dataflow.DataFlowService;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static java.lang.String.format;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v4alpha/dataflows")
public class DataFlowApiController implements DataFlowApi {

    private final Monitor monitor;
    private final DataFlowService service;

    public DataFlowApiController(Monitor monitor, DataFlowService service) {
        this.monitor = monitor;
        this.service = service;
    }

    @POST
    @Path("/{id}/trigger")
    @Override
    public void triggerDataTransferV4Alpha(@PathParam("id") String id) {
        service.trigger(id)
                .onSuccess(v -> monitor.debug(format("Trigger requested for dataflow with ID %s", id)))
                .orElseThrow(exceptionMapper(DataFlow.class, id));
    }

}
