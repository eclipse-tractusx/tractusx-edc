/********************************************************************************
 * Copyright (c) 2024 Robert Bosch Manufacturing Solutions GmbH and others
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.edc.dataplane.dtr.http.accesscontrol;

import com.fasterxml.jackson.databind.util.RawValue;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path("/http/access/error")
public class HttpAccessControlErrorApiController implements HttpAccessControlErrorApi {

    @GET
    @Path("/401")
    @Override
    public Response unauthorized(@QueryParam("reasonPhrase") final String reasonPhrase) {
        return Response.status(Response.Status.UNAUTHORIZED.getStatusCode(), reasonPhrase).build();
    }

    @GET
    @Path("/403")
    @Override
    public Response forbidden(@QueryParam("reasonPhrase") final String reasonPhrase) {
        return Response.status(Response.Status.FORBIDDEN.getStatusCode(), reasonPhrase).build();
    }

    @POST
    @Path("/200")
    @Override
    public Response ok(@RequestBody final String body) {
        return Response.ok(new RawValue(body)).build();
    }
}
