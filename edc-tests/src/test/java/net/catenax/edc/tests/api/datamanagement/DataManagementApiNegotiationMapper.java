/*
 *  Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Mercedes-Benz Tech Innovation GmbH - Initial API and Implementation
 *
 */

package net.catenax.edc.tests.api.datamanagement;

import java.util.Arrays;
import net.catenax.edc.tests.data.ContractNegotiation;
import net.catenax.edc.tests.data.ContractNegotiationState;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface DataManagementApiNegotiationMapper {
  DataManagementApiNegotiationMapper INSTANCE =
      Mappers.getMapper(DataManagementApiNegotiationMapper.class);

  default ContractNegotiation map(final DataManagementApiNegotiation negotiation) {
    if (negotiation == null) {
      return null;
    }

    final ContractNegotiationState state =
        Arrays.stream(ContractNegotiationState.values())
            .filter(option -> option.name().equals(negotiation.getState()))
            .findFirst()
            .orElse(ContractNegotiationState.UNKNOWN);

    return ContractNegotiation.builder()
        .id(negotiation.getId())
        .agreementId(negotiation.getAgreementId())
        .state(state)
        .build();
  }
}
