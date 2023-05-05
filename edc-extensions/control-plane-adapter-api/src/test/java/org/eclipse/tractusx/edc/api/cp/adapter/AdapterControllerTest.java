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

import io.restassured.specification.RequestSpecification;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.service.spi.result.ServiceResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.eclipse.tractusx.edc.api.cp.adapter.dto.TransferOpenRequestDto;
import org.eclipse.tractusx.edc.spi.cp.adapter.service.AdapterTransferProcessService;
import org.eclipse.tractusx.edc.spi.cp.adapter.types.TransferOpenRequest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.eclipse.tractusx.edc.api.cp.adapter.TestFunctions.openRequest;
import static org.eclipse.tractusx.edc.api.cp.adapter.TestFunctions.requestDto;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ApiTest
public class AdapterControllerTest extends RestControllerTestBase {

    public static final String ADAPTER_OPEN_TRANSFER_PATH = "/adapter/transfer/open";
    AdapterTransferProcessService adapterTransferProcessService = mock(AdapterTransferProcessService.class);

    TypeTransformerRegistry transformerRegistry = mock(TypeTransformerRegistry.class);

    @Test
    void openTransfer_shouldWork_whenValidRequest() {

        var openRequest = openRequest();
        when(transformerRegistry.transform(any(), eq(TransferOpenRequest.class))).thenReturn(Result.success(openRequest));
        when(adapterTransferProcessService.openTransfer(openRequest)).thenReturn(ServiceResult.success());

        TransferOpenRequestDto request = requestDto();


        baseRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .post(ADAPTER_OPEN_TRANSFER_PATH)
                .then()
                .statusCode(204);

    }

    @Test
    void openTransfer_shouldReturnBadRequest_whenValidInvalidRequest() {

        var request = TransferOpenRequestDto.Builder.newInstance().build();

        baseRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .post(ADAPTER_OPEN_TRANSFER_PATH)
                .then()
                .statusCode(400);

    }

    @Override
    protected Object controller() {
        return new AdapterController(adapterTransferProcessService, transformerRegistry);
    }


    private RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:" + port)
                .basePath("/")
                .when();
    }
}
