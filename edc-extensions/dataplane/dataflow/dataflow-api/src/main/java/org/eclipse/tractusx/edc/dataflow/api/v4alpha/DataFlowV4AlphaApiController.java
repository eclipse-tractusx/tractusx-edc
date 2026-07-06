/********************************************************************************
 * Copyright (c) 2025 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 * Copyright (c) 2026 Cofinity-X GmbH
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
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.tractusx.edc.dataflow.api.DataFlowApiController;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.api.ApiWarnings.deprecationWarning;

@Deprecated(since = "0.13.0")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v4alpha/dataflows")
public class DataFlowV4AlphaApiController implements DataFlowV4AlphaApi {

    private final DataFlowApiController delegate;
    private final Monitor monitor;

    public DataFlowV4AlphaApiController(DataFlowApiController delegate, Monitor monitor) {
        this.delegate = delegate;
        this.monitor = monitor;
    }

    @POST
    @Path("/{id}/trigger")
    @Override
    public void triggerDataTransferV4Alpha(@PathParam("id") String id) {
        monitor.warning(deprecationWarning("/v4alpha", "/v3"));
        delegate.trigger(id);
    }

}
