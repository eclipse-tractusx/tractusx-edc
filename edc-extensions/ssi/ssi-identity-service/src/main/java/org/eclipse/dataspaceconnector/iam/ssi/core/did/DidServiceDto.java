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
 *      ZF Friedrichshafen AG - Initial API and Implementation
 */

package org.eclipse.dataspaceconnector.iam.ssi.core.did;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class DidServiceDto {

  private final String id;

  private final String type;

  private final String serviceEndpoint;

  private final List<String> recipientKeys;

  private final List<String> routingKeys;

  private final int priority;

  public DidServiceDto(@JsonProperty("id") String id,
                       @JsonProperty("type") String type,
                       @JsonProperty("serviceEndpoint") String serviceEndpoint,
                       @JsonProperty("recipientKeys") List<String> recipientKeys,
                       @JsonProperty("routingKeys") List<String> routingKeys,
                       @JsonProperty("priority") int priority) {
    this.id = id;
    this.type = type;
    this.serviceEndpoint = serviceEndpoint;
    this.recipientKeys = recipientKeys;
    this.routingKeys = routingKeys;
    this.priority = priority;
  }

  public String getId() {
    return id;
  }

  public String getType() {
    return type;
  }

  public String getServiceEndpoint() {
    return serviceEndpoint;
  }

  public List<String> getRecipientKeys() {
    return recipientKeys;
  }

  public List<String> getRoutingKeys() {
    return routingKeys;
  }

  public int getPriority() {
    return priority;
  }
}
