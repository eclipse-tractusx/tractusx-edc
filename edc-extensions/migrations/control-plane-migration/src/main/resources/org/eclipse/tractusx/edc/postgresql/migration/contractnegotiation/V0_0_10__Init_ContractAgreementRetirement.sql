--
--  Copyright (c) 2024 Mercedes-Benz Tech Innovation GmbH
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
-- table: edc_contract_agreement
--
CREATE TABLE IF NOT EXISTS edc_contract_agreement
(
    id                VARCHAR      NOT NULL
        CONSTRAINT contract_agreement_pk PRIMARY KEY,
    provider_agent_id VARCHAR(255),
    consumer_agent_id VARCHAR(255),
    signing_date      BIGINT,
    start_date        BIGINT,
    end_date          INTEGER,
    asset_id          VARCHAR(255) NOT NULL,
    policy_id         VARCHAR(255),
    serialized_policy TEXT
);

CREATE TABLE IF NOT EXISTS edc_agreement_retirement
(
    contract_agreement_id     VARCHAR      NOT NULL UNIQUE
        CONSTRAINT agreement_retirement_contract_agreement_id_fk REFERENCES edc_contract_agreement,
    reason                    VARCHAR(255) NOT NULL,
    agreement_retirement_date BIGINT       NOT NULL
);