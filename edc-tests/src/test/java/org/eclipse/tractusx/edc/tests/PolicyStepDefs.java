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

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.eclipse.tractusx.edc.tests.data.*;

public class PolicyStepDefs {

  @Given("'{connector}' has the following policies")
  public void hasPolicies(Connector connector, DataTable table) throws Exception {
    var api = connector.getDataManagementAPI();
    var policies = table.asMaps().stream().map(this::parseRow).collect(toList());

    for (var policy : policies) api.createPolicy(policy);
  }

  private Policy parseRow(Map<String, String> row) {
    var id = row.get("id");
    var action = row.get("action");
    var constraints = new ArrayList<Constraint>();

    var businessPartnerNumber = row.get("businessPartnerNumber");
    if (businessPartnerNumber != null && !businessPartnerNumber.isBlank()) {
      var bpnConstraints =
          stream(businessPartnerNumber.split(","))
              .map(BusinessPartnerNumberConstraint::new)
              .collect(toList());
      constraints.add(new OrConstraint(bpnConstraints));
    }

    var payMe = row.get("payMe");
    if (payMe != null && !payMe.isBlank())
      constraints.add(new PayMeConstraint(Double.parseDouble(payMe)));

    var permission = new Permission(action, null, constraints);
    return new Policy(id, List.of(permission));
  }
}
