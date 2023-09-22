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

package org.eclipse.tractusx.edc.dataplane.proxy.consumer.api.asset;

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.edc.web.spi.ApiErrorDetail;

/**
 * Maps client errors to return the associated status.
 */
@Provider
public class ClientErrorExceptionMapper implements ExceptionMapper<ClientErrorException> {

    public ClientErrorExceptionMapper() {
    }

    @Override
    public Response toResponse(ClientErrorException exception) {
        var detail = ApiErrorDetail.Builder.newInstance().message(exception.getMessage()).build();
        return Response.status(exception.getResponse().getStatus()).entity(detail).build();
    }
}


