/*
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 * Copyright (c) 2021-2023 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.edc.provision.additionalheaders;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.connector.transfer.spi.types.ResourceDefinition;
import org.eclipse.edc.spi.types.domain.DataAddress;

@JsonDeserialize(builder = AdditionalHeadersResourceDefinition.Builder.class)
@JsonTypeName("dataspaceconnector:additionalheadersresourcedefinition")
class AdditionalHeadersResourceDefinition extends ResourceDefinition {

  private String contractId;
  private DataAddress dataAddress;

  @Override
  public Builder toBuilder() {
    return initializeBuilder(new Builder());
  }

  public DataAddress getDataAddress() {
    return dataAddress;
  }

  public String getContractId() {
    return contractId;
  }

  @JsonPOJOBuilder(withPrefix = "")
  public static class Builder
      extends ResourceDefinition.Builder<AdditionalHeadersResourceDefinition, Builder> {

    protected Builder() {
      super(new AdditionalHeadersResourceDefinition());
    }

    @JsonCreator
    public static Builder newInstance() {
      return new Builder();
    }

    public Builder contractId(String contractId) {
      this.resourceDefinition.contractId = contractId;
      return this;
    }

    public Builder dataAddress(DataAddress dataAddress) {
      this.resourceDefinition.dataAddress = dataAddress;
      return this;
    }
  }
}
