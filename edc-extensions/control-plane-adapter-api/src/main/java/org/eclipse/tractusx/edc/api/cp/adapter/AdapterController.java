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

package org.eclipse.tractusx.edc.api.cp.adapter;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.tractusx.edc.api.cp.adapter.dto.TransferOpenRequestDto;
import org.eclipse.tractusx.edc.spi.cp.adapter.service.AdapterTransferProcessService;
import org.eclipse.tractusx.edc.spi.cp.adapter.types.TransferOpenRequest;

import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@Path("/adapter/transfer")
public class AdapterController implements AdapterApi {

    private final AdapterTransferProcessService adapterTransferProcessService;

    private final TypeTransformerRegistry transformerRegistry;
    
    public AdapterController(AdapterTransferProcessService adapterTransferProcessService, TypeTransformerRegistry transformerRegistry) {
        this.adapterTransferProcessService = adapterTransferProcessService;
        this.transformerRegistry = transformerRegistry;
    }

    @POST
    @Path("/open")
    @Override
    public void openTransfer(TransferOpenRequestDto dto) {
        var transformResult = transformerRegistry.transform(dto, TransferOpenRequest.class)
                .orElseThrow(InvalidRequestException::new);

        adapterTransferProcessService.openTransfer(transformResult).orElseThrow(exceptionMapper(TransferOpenRequest.class));
    }
}
