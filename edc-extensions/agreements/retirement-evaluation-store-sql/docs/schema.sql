--
--  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
--
--  This program and the accompanying materials are made available under the
--  terms of the Apache License, Version 2.0 which is available at
--  https://www.apache.org/licenses/LICENSE-2.0
--
--  SPDX-License-Identifier: Apache-2.0
--
--  Contributors:
--        Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
--

--
-- table: edc_contract_agreement
--
CREATE TABLE IF NOT EXISTS edc_contract_agreement
(
    agreement_id            VARCHAR      NOT NULL
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

--
-- table: edc_contract_negotiation
--
CREATE TABLE IF NOT EXISTS edc_contract_negotiation
(
    agreement_id                VARCHAR(255)                                             NOT NULL
        CONSTRAINT contract_negotiation_pk PRIMARY KEY,
    correlation_id        VARCHAR(255)                                             NOT NULL,
    counterparty_id       VARCHAR(255)                                             NOT NULL,
    counterparty_address  VARCHAR(255)                                             NOT NULL,
    protocol              VARCHAR(255) DEFAULT 'ids-multipart':: CHARACTER VARYING NOT NULL,
    type                  INTEGER      DEFAULT 0                                   NOT NULL,
    state                 INTEGER      DEFAULT 0                                   NOT NULL,
    state_count           INTEGER      DEFAULT 0,
    state_timestamp       BIGINT,
    error_detail          TEXT,
    contract_agreement_id TEXT
        CONSTRAINT contract_negotiation_contract_agreement_id_fk REFERENCES edc_contract_agreement,
    contract_offers       TEXT,
    trace_context         TEXT
);

CREATE TABLE IF NOT EXISTS edc_agreement_retirement
(
    contract_agreement_id     VARCHAR      NOT NULL
        CONSTRAINT agreement_retirement_contract_agreement_id_pk PRIMARY KEY,
    reason                    VARCHAR(255) NOT NULL,
    agreement_retirement_date BIGINT       NOT NULL
);
