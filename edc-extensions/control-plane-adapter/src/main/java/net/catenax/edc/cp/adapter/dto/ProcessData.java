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
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;

@Getter
@ToString
@Builder
public class ProcessData {
  private final long timestamp = currentTimeMillis();

  // request data
  private final String assetId;
  private final String provider;
  private String contractOfferId;
  private int catalogExpiryTime;
  private boolean contractAgreementCacheOn;

  // contract data
  @Setter private String contractNegotiationId;
  @Setter private String contractAgreementId;
  @Builder.Default @Setter private boolean isContractConfirmed = false;

  // result/response data
  @Setter private EndpointDataReference endpointDataReference;
  @Setter private String errorMessage;
  @Setter private Response.Status errorStatus;
}
