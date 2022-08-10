#
#  Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
#
#  This program and the accompanying materials are made available under the
#  terms of the Apache License, Version 2.0 which is available at
#  https://www.apache.org/licenses/LICENSE-2.0
#
#  SPDX-License-Identifier: Apache-2.0
#
#  Contributors:
#       Mercedes-Benz Tech Innovation GmbH - Initial API and Implementation
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
      | id            | action | payMe      |
      | policy-1      | USE    |            |
      | policy-pay-me | USE    | 1000 |
    And 'Plato' has the following contract definitions
      | id                    | access policy | contract policy | asset   |
      | contract-definition-1 | policy-1      | policy-pay-me   | asset-1 |
    When 'Sokrates' sends 'Plato' a counter offer without constraints
      | definition id              | asset id  |
      | contract-definition-1      | asset-1   |
    # Then the negotiation is declined # Issue https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/issues/1791
