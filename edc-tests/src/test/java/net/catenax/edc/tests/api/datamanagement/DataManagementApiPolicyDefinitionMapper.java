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

import net.catenax.edc.tests.data.Policy;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface DataManagementApiPolicyDefinitionMapper {
  DataManagementApiPolicyDefinitionMapper INSTANCE =
      Mappers.getMapper(DataManagementApiPolicyDefinitionMapper.class);

  default DataManagementApiPolicyDefinition map(Policy policy) {
    if (policy == null) {
      return null;
    }

    final DataManagementApiPolicyDefinition apiObject = new DataManagementApiPolicyDefinition();
    apiObject.setUid(policy.getId());
    apiObject.setPolicy(DataManagementApiPolicyMapper.INSTANCE.map(policy));
    return apiObject;
  }
}
