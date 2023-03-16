#
#  Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
#  Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
#
#  See the NOTICE file(s) distributed with this work for additional
#  information regarding copyright ownership.
#
#  This program and the accompanying materials are made available under the
#  terms of the Apache License, Version 2.0 which is available at
#  https://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
#  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
#  License for the specific language governing permissions and limitations
#  under the License.
#
#  SPDX-License-Identifier: Apache-2.0
#

Feature: Contract Negotiation

  Background: The Connector State
    Given 'Plato' has an empty database
    Given 'Sokrates' has an empty database

  Scenario: Counter Offers are rejected
    Given 'Plato' has the following assets
      | id      | description   |
      | asset-1 | Example Asset |
    And 'Plato' has the following policies
      | id            | action | payMe |
      | policy-1      | USE    |       |
      | policy-pay-me | USE    | 1000  |
    And 'Plato' has the following contract definitions
      | id                    | access policy | contract policy | asset   |
      | contract-definition-1 | policy-1      | policy-pay-me   | asset-1 |
    When 'Sokrates' sends 'Plato' an offer without constraints
      | definition id         | asset id |
      | contract-definition-1 | asset-1  |
    Then the negotiation is declined


  Scenario: An offer is rejected
    Given 'Plato' has the following assets
      | id      | description   |
      | asset-1 | Example Asset |
    And 'Plato' has the following policies
      | id            | action | payMe |
      | policy-1      | USE    |       |
      | policy-pay-me | USE    | 1000  |
    And 'Plato' has the following contract definitions
      | id                    | access policy | contract policy | asset   | validity |
      | contract-definition-1 | policy-1      | policy-pay-me   | asset-1 | 1        |
    When 'Sokrates' sends 'Plato' an offer without constraints
      | definition id         | asset id |
      | contract-definition-1 | asset-1  |
    Then the negotiation is declined