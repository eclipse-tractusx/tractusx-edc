--
--  Copyright (c) 2025 Cofinity-X GmbH
--
--  This program and the accompanying materials are made available under the
--  terms of the Apache License, Version 2.0 which is available at
--  https://www.apache.org/licenses/LICENSE-2.0
--
--  SPDX-License-Identifier: Apache-2.0
--
--  Contributors:
--       Cofinity-X GmbH
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

INSERT INTO edc_contract_agreement (id, provider_agent_id, consumer_agent_id, signing_date, start_date, end_date, asset_id, policy_id, serialized_policy)
VALUES
    ('test-agreement-id', 'default-provider-agent', 'default-consumer-agent', 0, 0, 0, 'default-asset', 'default-policy-id', '{}');

CREATE TABLE IF NOT EXISTS edc_contract_agreement_bpns
(
    agreement_id VARCHAR
        CONSTRAINT contract_agreement_bpns_contract_agreement_id_fk PRIMARY KEY
            REFERENCES edc_contract_agreement,
    provider_bpn VARCHAR(255) NOT NULL,
    consumer_bpn VARCHAR(255) NOT NULL
);