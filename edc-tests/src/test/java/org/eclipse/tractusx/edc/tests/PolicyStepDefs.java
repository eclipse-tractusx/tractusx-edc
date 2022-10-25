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

package org.eclipse.tractusx.edc.tests;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.eclipse.tractusx.edc.tests.data.AttributeConstraint;
import org.eclipse.tractusx.edc.tests.data.BusinessPartnerNumberConstraint;
import org.eclipse.tractusx.edc.tests.data.Constraint;
import org.eclipse.tractusx.edc.tests.data.PayMeConstraint;
import org.eclipse.tractusx.edc.tests.data.Permission;
import org.eclipse.tractusx.edc.tests.data.Policy;
import org.eclipse.tractusx.edc.tests.data.RoleConstraint;

public class PolicyStepDefs {

  @Given("'{connector}' has the following policies")
  public void hasPolicies(Connector connector, DataTable table) throws Exception {
    final DataManagementAPI api = connector.getDataManagementAPI();
    final List<Policy> policies = parseDataTable(table);

    for (Policy policy : policies) api.createPolicy(policy);
  }

  private List<Policy> parseDataTable(DataTable table) {
    final List<Policy> policies = new ArrayList<>();

    for (Map<String, String> map : table.asMaps()) {
      final String id = map.get("id");
      final String action = map.get("action");

      List<Constraint> constraints = new ArrayList<>();
      // BPN
      final String businessPartnerNumber = map.get("businessPartnerNumber");
      if (businessPartnerNumber != null && !businessPartnerNumber.isBlank())
        constraints.add(new BusinessPartnerNumberConstraint(businessPartnerNumber));
      // ROLE
      final String role = map.get("role");
      if (role != null && !role.isBlank()) constraints.add(new RoleConstraint(role));
      // ATTRIBUTE
      final String attribute = map.get("attribute");
      if (attribute != null && !attribute.isBlank())
        constraints.add(new AttributeConstraint(attribute));

      final String payMe = map.get("payMe");
      if (payMe != null && !payMe.isBlank())
        constraints.add(new PayMeConstraint(Double.parseDouble(payMe)));

      final List<Permission> permission = List.of(new Permission(action, null, constraints));

      policies.add(new Policy(id, permission));
    }

    return policies;
  }
}
