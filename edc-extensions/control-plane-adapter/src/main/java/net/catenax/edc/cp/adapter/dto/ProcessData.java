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

package net.catenax.edc.cp.adapter.dto;

import static java.lang.System.currentTimeMillis;

import jakarta.ws.rs.core.Response;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReference;

@Getter
@ToString
public class ProcessData {
  private final long timestamp = currentTimeMillis();

  // request data
  private final String assetId;
  private final String provider;
  private String contractOfferId;

  // contract data
  @Setter private String contractNegotiationId;
  @Setter private String contractAgreementId;
  @Setter private boolean isContractConfirmed = false;

  // result data
  @Setter private EndpointDataReference endpointDataReference;
  @Setter private String errorMessage;
  @Setter private Response.Status errorStatus;

  public ProcessData(String assetId, String provider) {
    this.assetId = assetId;
    this.provider = provider;
  }

  public ProcessData(String assetId, String provider, String contractOfferId) {
    this.assetId = assetId;
    this.provider = provider;
    this.contractOfferId = contractOfferId;
  }
}
