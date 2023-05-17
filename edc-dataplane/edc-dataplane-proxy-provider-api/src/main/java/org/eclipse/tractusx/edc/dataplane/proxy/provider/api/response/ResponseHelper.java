/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

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
     * Creates a response with a message encoded for the given media type. Currently, <code>APPLICATION_JSON</code> and <code>TEXT_PLAIN</code> are supported.
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
