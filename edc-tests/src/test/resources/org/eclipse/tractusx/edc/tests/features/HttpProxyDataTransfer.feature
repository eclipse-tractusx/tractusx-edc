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

Feature: HttpProxy Data Transfer

  Background: The Connector State
    Given 'Plato' has an empty database
    Given 'Sokrates' has an empty database

  Scenario: Connector transfers data via HttpProxy
    Given 'Plato' has a http proxy assets
      | id      | description               | baseUrl                          |
      | asset-1 | http proxy transfer asset | http://localhost:8081/api/health |
    And 'Plato' has the following policies
      | id       | action |
      | policy-1 | USE    |
    And 'Plato' has the following contract definitions
      | id                    | access policy | contract policy | asset   |
      | contract-definition-1 | policy-1      | policy-1        | asset-1 |
    When 'Sokrates' negotiates the contract successfully with 'Plato'
      | contract offer id     | asset id | policy id |
      | contract-definition-1 | asset-1  | policy-1  |
    And 'Sokrates' initiates HttpProxy transfer from 'Plato'
      | asset id | receiverHttpEndpoint |
      | asset-1  | http://backend:8080  |
    Then the backend application of 'Sokrates' has received data

  Scenario: Connector transfers data via HttpProxy, data on provider side requires oauth2 authentication
    Given 'Plato' has a http proxy assets
      | id      | description               | baseUrl                          | oauth2 token url     | oauth2 client id  | oauth2 client secret | oauth2 scope |
      | asset-1 | http proxy transfer asset | http://localhost:8081/api/health | http://ids-daps:4567 | data-plane-oauth2 | supersecret          | openid       |
    And 'Plato' has the following policies
      | id       | action |
      | policy-1 | USE    |
    And 'Plato' has the following contract definitions
      | id                    | access policy | contract policy | asset   |
      | contract-definition-1 | policy-1      | policy-1        | asset-1 |
    When 'Sokrates' negotiates the contract successfully with 'Plato'
      | contract offer id     | asset id | policy id |
      | contract-definition-1 | asset-1  | policy-1  |
    And 'Sokrates' initiates HttpProxy transfer from 'Plato'
      | asset id |
      | asset-1  |
    Then the backend application of 'Sokrates' has received data
