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

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import net.catenax.edc.tests.data.Permission;
import net.catenax.edc.tests.data.Policy;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface DataManagementApiPolicyMapper {
  DataManagementApiPolicyMapper INSTANCE = Mappers.getMapper(DataManagementApiPolicyMapper.class);

  default Policy map(DataManagementApiPolicy dataManagementApiPolicy) {
    if (dataManagementApiPolicy == null) {
      return null;
    }

    final String id = dataManagementApiPolicy.getUid();
    final List<Permission> permissions =
        dataManagementApiPolicy.getPermissions().stream()
            .map(DataManagementApiPermissionMapper.INSTANCE::map)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

    return Policy.builder().id(id).permission(permissions).build();
  }

  default DataManagementApiPolicy map(Policy policy) {
    if (policy == null) {
      return null;
    }

    final List<DataManagementApiPermission> permissions =
        policy.getPermission().stream()
            .map(DataManagementApiPermissionMapper.INSTANCE::map)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    final DataManagementApiPolicy dataManagementApiPolicy = new DataManagementApiPolicy();
    dataManagementApiPolicy.setPermissions(permissions);

    return dataManagementApiPolicy;
  }
}
