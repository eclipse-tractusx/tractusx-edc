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

package net.catenax.edc.tests.stepdefs;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import net.catenax.edc.tests.Connector;
import net.catenax.edc.tests.api.datamanagement.DataManagementApiClient;
import net.catenax.edc.tests.data.BusinessPartnerNumberConstraint;
import net.catenax.edc.tests.data.Constraint;
import net.catenax.edc.tests.data.PayMeConstraint;
import net.catenax.edc.tests.data.Permission;
import net.catenax.edc.tests.data.Policy;

public class PolicyStepDefs {

  @Given("'{connector}' has the following policies")
  public void hasPolicies(@NonNull final Connector connector, @NonNull final DataTable table) {
    final DataManagementApiClient api = connector.getDataManagementApiClient();
    parseDataTable(table).forEach(api::createPolicy);
  }

  private List<Policy> parseDataTable(final DataTable table) {
    final List<Policy> policies = new ArrayList<>();

    for (Map<String, String> map : table.asMaps()) {
      final String id = map.get("id");
      final String action = map.get("action");

      final List<Constraint> constraints = new ArrayList<>();
      final String businessPartnerNumber = map.get("businessPartnerNumber");
      if (businessPartnerNumber != null && !businessPartnerNumber.isBlank()) {
        constraints.add(
            BusinessPartnerNumberConstraint.builder()
                .businessPartnerNumber(businessPartnerNumber)
                .build());
      }

      final String payMe = map.get("payMe");
      if (payMe != null && !payMe.isBlank()) {
        constraints.add(new PayMeConstraint(Double.parseDouble(payMe)));
      }

      final List<Permission> permission =
          Collections.singletonList(
              Permission.builder().action(action).target(null).constraints(constraints).build());

      policies.add(Policy.builder().id(id).permission(permission).build());
    }

    return policies;
  }
}
