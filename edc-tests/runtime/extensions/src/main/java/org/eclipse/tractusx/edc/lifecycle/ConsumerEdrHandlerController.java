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

package org.eclipse.tractusx.edc.lifecycle;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Path("/consumer")
public class ConsumerEdrHandlerController {

    private final Monitor monitor;
    private final Map<String, EndpointDataReference> dataReference;

    public ConsumerEdrHandlerController(Monitor monitor) {
        this.monitor = monitor;
        dataReference = new HashMap<>();
    }

    @Path("/datareference")
    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    public void pushDataReference(EndpointDataReference edr) {
        monitor.debug("Received new endpoint data reference with url " + edr.getEndpoint());
        dataReference.put(edr.getId(), edr);
    }

    @Path("/datareference/{id}")
    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public EndpointDataReference getDataReference(@PathParam("id") String id) {
        return Optional.ofNullable(dataReference.get(id)).orElseGet(() -> {
            monitor.warning("No EndpointDataReference found with id " + id);
            return null;
        });
    }

}
