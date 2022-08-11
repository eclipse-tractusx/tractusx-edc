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

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import net.catenax.edc.tests.data.Permission;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface DataManagementApiPermissionMapper {
  DataManagementApiPermissionMapper INSTANCE =
      Mappers.getMapper(DataManagementApiPermissionMapper.class);

  default Permission map(final DataManagementApiPermission dataManagementApiPermission) {
    if (dataManagementApiPermission == null) {
      return null;
    }

    final String target = dataManagementApiPermission.getTarget();
    final String action = dataManagementApiPermission.getAction().getType();
    return Permission.builder()
        .action(action)
        .target(target)
        .constraints(Collections.emptyList())
        .build();
  }

  default DataManagementApiPermission map(final Permission permission) {
    if (permission == null) {
      return null;
    }

    final String target = permission.getTarget();
    final String action = permission.getAction();

    final DataManagementApiRuleAction apiAction = new DataManagementApiRuleAction();
    apiAction.setType(action);

    final List<DataManagementApiConstraint> constraints =
        permission.getConstraints().stream()
            .map(DataManagementApiConstraintMapper.INSTANCE::map)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

    final DataManagementApiPermission apiObject = new DataManagementApiPermission();
    apiObject.setTarget(target);
    apiObject.setAction(apiAction);
    apiObject.setConstraints(constraints);
    return apiObject;
  }
}
