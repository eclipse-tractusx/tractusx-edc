#  Copyright (c) 2022 ZF Friedrichshafen AG
# Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
#
# See the NOTICE file(s) distributed with this work for additional
# information regarding copyright ownership.
#
# This program and the accompanying materials are made available under the
# terms of the Apache License, Version 2.0 which is available at
# https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations
# under the License.
#
# SPDX-License-Identifier: Apache-2.0
#

Feature: S3 File Transfer

  Background: The Connector State
    Given 'Plato' has an empty database
    Given 'Sokrates' has an empty database
    Given 'Sokrates' has an empty storage bucket called 'destinationbucket'
    Given 'Plato' has a storage bucket called 'sourcebucket' with the file called 'testfile'

  Scenario: Request file transfer via S3
    Given 'Plato' has the following S3 assets
      | id      | description   | data_address_type | data_address_s3_bucket_name | data_address_s3_key_name | data_address_s3_region |
      | asset-1 | Example Asset | AmazonS3          | sourcebucket                | testfile                 | us-east-1              |
    And 'Plato' has the following policies
      | id       | action | payMe |
      | policy-1 | USE    |       |
    And 'Plato' has the following contract definitions
      | id                    | access policy | contract policy | asset   |
      | contract-definition-1 | policy-1      | policy-1        | asset-1 |
    When 'Sokrates' requests the catalog from 'Plato'
    Then the catalog contains the following offers
      | source definition     | asset   |
      | contract-definition-1 | asset-1 |
    Then 'Sokrates' negotiates the contract successfully with 'Plato'
      | contract offer id     | asset id | policy id |
      | contract-definition-1 | asset-1  | policy-1  |
    Then 'Sokrates' initiate S3 transfer process from 'Plato'
      | data_address_s3_bucket_name | data_address_s3_key_name | data_address_s3_region |
      | destinationbucket           | testfile                 | us-east-1              |
    Then 'Sokrates' has a storage bucket called 'destinationbucket' with transferred file called 'testfile'
