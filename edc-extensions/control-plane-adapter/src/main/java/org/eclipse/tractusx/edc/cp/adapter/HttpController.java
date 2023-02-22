/*
 * Copyright (c) 2022 ZF Friedrichshafen AG
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 * ZF Friedrichshafen AG - Initial API and Implementation
 *
 */

package org.eclipse.tractusx.edc.cp.adapter;

import static java.util.Objects.isNull;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.util.string.StringUtils;
import org.eclipse.tractusx.edc.cp.adapter.dto.DataReferenceRetrievalDto;
import org.eclipse.tractusx.edc.cp.adapter.dto.ProcessData;
import org.eclipse.tractusx.edc.cp.adapter.messaging.Channel;
import org.eclipse.tractusx.edc.cp.adapter.messaging.Message;
import org.eclipse.tractusx.edc.cp.adapter.messaging.MessageBus;
import org.eclipse.tractusx.edc.cp.adapter.service.ResultService;

@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path("/adapter/asset")
@RequiredArgsConstructor
public class HttpController {
  private final Monitor monitor;
  private final ResultService resultService;
  private final MessageBus messageBus;
  private final ApiAdapterConfig config;

  @GET
  @Path("sync/{assetId}")
  public Response getAssetSynchronous(
      @PathParam("assetId") String assetId,
      @QueryParam("providerUrl") String providerUrl,
      @QueryParam("contractAgreementId") String contractAgreementId,
      @QueryParam("contractAgreementReuse") @DefaultValue("true") boolean contractAgreementReuse,
      @QueryParam("timeout") String timeout) {

    if (invalidParams(assetId, providerUrl)) {
      return badRequestResponse();
    }

    String traceId =
        initiateProcess(assetId, providerUrl, contractAgreementId, contractAgreementReuse);

    try {
      ProcessData processData =
          StringUtils.isNullOrEmpty(timeout) || !isNumeric(timeout)
              ? resultService.pull(traceId)
              : resultService.pull(traceId, Long.parseLong(timeout), TimeUnit.SECONDS);

      if (Objects.isNull(processData)) {
        return notFoundResponse();
      }
      if (Objects.nonNull(processData.getErrorStatus())) {
        return errorResponse(processData);
      }
      if (Objects.nonNull(processData.getEndpointDataReference())) {
        return okResponse(processData);
      }
      return timeoutResponse();
    } catch (InterruptedException e) {
      monitor.severe("InterruptedException", e);
      return notFoundResponse();
    }
  }

  private Response badRequestResponse() {
    return Response.status(Response.Status.BAD_REQUEST)
        .entity("AssetId or providerUrl is empty!")
        .build();
  }

  private boolean invalidParams(String assetId, String providerUrl) {
    return isNull(assetId) || assetId.isBlank() || isNull(providerUrl) || providerUrl.isBlank();
  }

  private String initiateProcess(
      String assetId,
      String providerUrl,
      String contractAgreementId,
      boolean contractAgreementReuse) {
    ProcessData processData =
        ProcessData.builder()
            .assetId(assetId)
            .provider(providerUrl)
            .contractAgreementId(contractAgreementId)
            .contractAgreementReuseOn(isContractAgreementReuseOn(contractAgreementReuse))
            .catalogExpiryTime(config.getCatalogExpireAfterTime())
            .build();

    Message<ProcessData> message =
        new DataReferenceRetrievalDto(processData, config.getDefaultMessageRetryNumber());
    messageBus.send(Channel.INITIAL, message);
    return message.getTraceId();
  }

  private boolean isContractAgreementReuseOn(boolean contractAgreementReuse) {
    return contractAgreementReuse && config.isContractAgreementReuseOn();
  }

  private Response notFoundResponse() {
    return Response.status(Response.Status.NOT_FOUND)
        .entity(Response.Status.NOT_FOUND.getReasonPhrase())
        .build();
  }

  private Response errorResponse(ProcessData processData) {
    return Response.status(processData.getErrorStatus())
        .entity(processData.getErrorMessage())
        .build();
  }

  private Response okResponse(ProcessData processData) {
    return Response.status(Response.Status.OK)
        .entity(processData.getEndpointDataReference())
        .build();
  }

  private Response timeoutResponse() {
    return Response.status(Response.Status.REQUEST_TIMEOUT)
        .entity(Response.Status.REQUEST_TIMEOUT.getReasonPhrase())
        .build();
  }

  private boolean isNumeric(String str) {
    return str != null && str.matches("[0-9]+");
  }
}
