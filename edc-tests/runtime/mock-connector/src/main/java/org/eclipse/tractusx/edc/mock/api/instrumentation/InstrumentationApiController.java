/*
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft
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

package org.eclipse.tractusx.edc.mock.api.instrumentation;


import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.tractusx.edc.mock.RecordedRequest;
import org.eclipse.tractusx.edc.mock.ResponseQueue;

import java.util.List;

/**
 * Instrumentation controller for the mock connector.
 *
 * @deprecated since 0.11.0
 */
@Deprecated(since = "0.11.0")
@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@Path("/instrumentation")
public class InstrumentationApiController implements InstrumentationApi {

    private final ResponseQueue responseQueue;

    public InstrumentationApiController(ResponseQueue responseQueue) {
        this.responseQueue = responseQueue;
    }

    @Override
    @POST
    public void addNewRequest(RecordedRequest<?, ?> recordedRequest) {
        responseQueue.append(recordedRequest);
    }

    @Override
    @DELETE
    public void clearQueue() {
        responseQueue.clear();
    }

    @Override
    @GET
    public List<RecordedRequest<?, ?>> getRequests() {
        return responseQueue.toList();
    }

    @Override
    @GET
    @Path("/count")
    public int count() {
        return responseQueue.toList().size();
    }
}
