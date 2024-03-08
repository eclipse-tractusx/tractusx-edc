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

package org.eclipse.tractusx.edc.dataplane.proxy.provider.api.response;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jetbrains.annotations.Nullable;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;
import static jakarta.ws.rs.core.Response.status;
import static java.lang.String.format;

/**
 * Utility functions for creating responses.
 */
public class ResponseHelper {

    /**
     * Creates a response with a message encoded for the given media type. Currently, {@code APPLICATION_JSON} and {@code TEXT_PLAIN} are supported.
     */
    public static Response createMessageResponse(Response.Status status, String message, @Nullable MediaType mediaType) {
        if (mediaType != null && APPLICATION_JSON.equals(mediaType.toString())) {
            return status(status).entity(format("'%s'", message)).type(APPLICATION_JSON).build();
        } else {
            return status(status).entity(format("%s", message)).type(TEXT_PLAIN).build();
        }
    }

    private ResponseHelper() {
    }
}
