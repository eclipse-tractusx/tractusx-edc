--
--  Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
--
--  This program and the accompanying materials are made available under the
--  terms of the Apache License, Version 2.0 which is available at
--  https://www.apache.org/licenses/LICENSE-2.0
--
--  SPDX-License-Identifier: Apache-2.0
--
--  Contributors:
--       Mercedes-Benz Tech Innovation GmbH - Initial Database Schema
--

--
-- table: edc_contract_definitions
--
CREATE TABLE IF NOT EXISTS edc_contract_definitions
(
    contract_definition_id VARCHAR(255) NOT NULL,
    access_policy          TEXT         NOT NULL,
    contract_policy        TEXT         NOT NULL,
    selector_expression    TEXT         NOT NULL,
    PRIMARY KEY (contract_definition_id)
);
