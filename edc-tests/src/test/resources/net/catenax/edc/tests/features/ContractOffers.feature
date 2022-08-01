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

Feature: Contract Offers

  Background: The Connector State
    Given 'Plato' has no contract definitions
    Given 'Plato' has no policies
    Given 'Plato' has no assets

  Scenario: Catalog Request
    Given 'Plato' has the following assets
      | id      | description   |
      | asset-1 | Example Asset |
      | asset-2 | Example Asset |
    And 'Plato' has the following policies
      | id       | action |
      | policy-1 | USE    |
    And 'Plato' has the following contract definitions
      | id                    | access policy | contract policy | asset   |
      | contract-definition-1 | policy-1      | policy-1        | asset-1 |
      | contract-definition-2 | policy-1      | policy-1        | asset-2 |
    When 'Sokrates' requests the catalog from 'Plato'
    Then the catalog contains the following offers
      | source definition     | asset   |
      | contract-definition-1 | asset-1 |
      | contract-definition-2 | asset-2 |

  Scenario: EQ Business Partner Constrain for Catalog
    Given 'Plato' has the following assets
      | id      | description   |
      | asset-1 | Example Asset |
      | asset-2 | Example Asset |
      | asset-3 | Example Asset |
    And 'Plato' has the following policies
      | id       | action | businessPartnerNumber |
      | policy-1 | USE    |                       |
      | policy-2 | USE    | BPNFOO                |
      | policy-3 | USE    | BPNSOKRATES           |
    And 'Plato' has the following contract definitions
      | id                    | access policy | contract policy | asset   |
      | contract-definition-1 | policy-1      | policy-1        | asset-1 |
      | contract-definition-2 | policy-2      | policy-1        | asset-2 |
      | contract-definition-3 | policy-3      | policy-1        | asset-3 |
    When 'Sokrates' requests the catalog from 'Plato'
    Then the catalog contains the following offers
      | source definition     | asset   |
      | contract-definition-1 | asset-1 |
      | contract-definition-3 | asset-3 |
    Then the catalog does not contain the following offers
      | source definition     | asset   |
      | contract-definition-2 | asset-2 |

  Scenario: Multiple Contract Offers for same Asset
    Given 'Plato' has the following assets
      | id      | description   |
      | asset-1 | Example Asset |
    And 'Plato' has the following policies
      | id       | action | businessPartnerNumber |
      | policy-1 | USE    |                       |
      | policy-2 | USE    | BPNSOKRATES           |
    And 'Plato' has the following contract definitions
      | id                    | access policy | contract policy | asset   |
      | contract-definition-1 | policy-1      | policy-1        | asset-1 |
      | contract-definition-2 | policy-2      | policy-1        | asset-1 |
    When 'Sokrates' requests the catalog from 'Plato'
    Then the catalog contains the following offers
      | source definition     | asset   |
      | contract-definition-2 | asset-1 |
      #| contract-definition-1 | asset-1 | # Issue https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/issues/1764
