##
##  Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
##
##  This program and the accompanying materials are made available under the
##  terms of the Apache License, Version 2.0 which is available at
##  https://www.apache.org/licenses/LICENSE-2.0
##
##  SPDX-License-Identifier: Apache-2.0
##
##  Contributors:
##       Mercedes-Benz Tech Innovation GmbH - Initial API and Implementation
##

Feature: Data Transfer

  Background: The Connector State
    Given 'Plato' has an empty database
    Given 'Sokrates' has an empty database
    Given 'Plato' has an empty backend
    Given 'Sokrates' has an empty backend

  Scenario: Negotiation completed and data transfer performed
    Given 'Plato' has the following assets
      | id      | description   |
      | asset-1 | Example Asset |
    And 'Plato' has the following policies
      | id            | action |
      | policy-1      | USE    |
    And 'Plato' has the following contract definitions
      | id                    | access policy | contract policy | asset   |
      | contract-definition-1 | policy-1      | policy-1        | asset-1 |
