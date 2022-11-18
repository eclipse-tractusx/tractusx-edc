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

package net.catenax.edc.cp.adapter;

import static java.util.Objects.isNull;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import net.catenax.edc.cp.adapter.dto.DataReferenceRetrievalDto;
import net.catenax.edc.cp.adapter.dto.ProcessData;
import net.catenax.edc.cp.adapter.messaging.Channel;
import net.catenax.edc.cp.adapter.messaging.Message;
import net.catenax.edc.cp.adapter.messaging.MessageBus;
import net.catenax.edc.cp.adapter.service.ResultService;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;

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
      @PathParam("assetId") String assetId, @QueryParam("providerUrl") String providerUrl) {

    if (invalidParams(assetId, providerUrl)) {
      return badRequestResponse();
    }

    String traceId = initiateProcess(assetId, providerUrl);

    try {
      ProcessData processData = resultService.pull(traceId);

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

  private String initiateProcess(String assetId, String providerUrl) {
    ProcessData processData = getProcessData(assetId, providerUrl);
    Message<ProcessData> message =
        new DataReferenceRetrievalDto(processData, config.getDefaultMessageRetryNumber());
    messageBus.send(Channel.INITIAL, message);
    return message.getTraceId();
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

  private ProcessData getProcessData(String assetId, String providerUrl) {
    return ProcessData.builder()
        .assetId(assetId)
        .provider(providerUrl)
        .contractAgreementCacheOn(config.isContractAgreementCacheOn())
        .catalogExpiryTime(config.getCatalogExpireAfterTime())
        .build();
  }
}
