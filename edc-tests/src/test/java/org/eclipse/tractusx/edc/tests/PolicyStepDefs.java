/*
 * Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
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

package org.eclipse.tractusx.edc.tests;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.eclipse.tractusx.edc.tests.data.BusinessPartnerNumberConstraint;
import org.eclipse.tractusx.edc.tests.data.Constraint;
import org.eclipse.tractusx.edc.tests.data.PayMeConstraint;
import org.eclipse.tractusx.edc.tests.data.Permission;
import org.eclipse.tractusx.edc.tests.data.Policy;

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
      final String businessPartnerNumber = map.get("businessPartnerNumber");
      if (businessPartnerNumber != null && !businessPartnerNumber.isBlank())
        constraints.add(new BusinessPartnerNumberConstraint(businessPartnerNumber));

      final String payMe = map.get("payMe");
      if (payMe != null && !payMe.isBlank())
        constraints.add(new PayMeConstraint(Double.parseDouble(payMe)));

      final List<Permission> permission = List.of(new Permission(action, null, constraints));

      policies.add(new Policy(id, permission));
    }

    return policies;
  }
}
