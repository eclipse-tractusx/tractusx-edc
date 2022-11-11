/*
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.eclipse.tractusx.edc.ssi.core.did;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class DidServiceDto {

  private final String id;

  private final String type;

  private final String serviceEndpoint;

  private final List<String> recipientKeys;

  private final List<String> routingKeys;

  private final int priority;

  public DidServiceDto(
      @JsonProperty("id") String id,
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
