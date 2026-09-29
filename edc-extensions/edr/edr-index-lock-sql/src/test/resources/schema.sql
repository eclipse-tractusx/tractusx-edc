--
-- Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft
--
-- See the NOTICE file(s) distributed with this work for additional
-- information regarding copyright ownership.
--
-- This program and the accompanying materials are made available under the
-- terms of the Apache License, Version 2.0 which is available at
-- https://www.apache.org/licenses/LICENSE-2.0.
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
-- WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
-- License for the specific language governing permissions and limitations
-- under the License.
--
-- SPDX-License-Identifier: Apache-2.0
--

CREATE TABLE IF NOT EXISTS edc_edr_entry
(
    transfer_process_id           VARCHAR NOT NULL PRIMARY KEY,
    agreement_id                  VARCHAR NOT NULL,
    asset_id                      VARCHAR NOT NULL,
    provider_id                   VARCHAR NOT NULL,
    contract_negotiation_id       VARCHAR,
    created_at                    BIGINT  NOT NULL,
    participant_context_id        VARCHAR
);
