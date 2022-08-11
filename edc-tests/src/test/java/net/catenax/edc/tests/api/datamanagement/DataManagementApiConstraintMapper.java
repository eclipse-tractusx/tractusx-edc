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

import net.catenax.edc.tests.data.BusinessPartnerNumberConstraint;
import net.catenax.edc.tests.data.Constraint;
import net.catenax.edc.tests.data.PayMeConstraint;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface DataManagementApiConstraintMapper {
  DataManagementApiConstraintMapper INSTANCE =
      Mappers.getMapper(DataManagementApiConstraintMapper.class);

  default DataManagementApiConstraint map(final Constraint constraint) {
    if (constraint == null) {
      return null;
    }

    if (constraint instanceof BusinessPartnerNumberConstraint) {
      return mapBusinessPartnerNumberConstraint((BusinessPartnerNumberConstraint) constraint);
    } else if (constraint instanceof PayMeConstraint) {
      return mapPayMeConstraint((PayMeConstraint) constraint);
    } else {
      throw new UnsupportedOperationException(
          "Unsupported constraint type: " + constraint.getClass().getName());
    }
  }

  default DataManagementApiConstraint mapBusinessPartnerNumberConstraint(
      final BusinessPartnerNumberConstraint constraint) {
    if (constraint == null) {
      return null;
    }

    final DataManagementApiLiteralExpression leftExpression =
        new DataManagementApiLiteralExpression();
    leftExpression.setValue("BusinessPartnerNumber");

    final DataManagementApiLiteralExpression rightExpression =
        new DataManagementApiLiteralExpression();
    rightExpression.setValue(constraint.getBusinessPartnerNumber());

    final DataManagementApiConstraint dataManagementApiConstraint =
        new DataManagementApiConstraint();
    dataManagementApiConstraint.setLeftExpression(leftExpression);
    dataManagementApiConstraint.setRightExpression(rightExpression);
    dataManagementApiConstraint.setOperator("EQ");

    return dataManagementApiConstraint;
  }

  default DataManagementApiConstraint mapPayMeConstraint(PayMeConstraint constraint) {
    if (constraint == null) {
      return null;
    }

    final DataManagementApiLiteralExpression leftExpression =
        new DataManagementApiLiteralExpression();
    leftExpression.setValue("PayMe");

    final DataManagementApiLiteralExpression rightExpression =
        new DataManagementApiLiteralExpression();
    rightExpression.setValue(String.valueOf(constraint.getAmount()));

    final DataManagementApiConstraint dataManagementApiConstraint =
        new DataManagementApiConstraint();
    dataManagementApiConstraint.setLeftExpression(leftExpression);
    dataManagementApiConstraint.setRightExpression(rightExpression);
    dataManagementApiConstraint.setOperator("EQ");

    return dataManagementApiConstraint;
  }
}
