--
--  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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


CREATE TABLE IF NOT EXISTS edc_lease
(
    leased_by      VARCHAR               NOT NULL,
    leased_at      BIGINT,
    lease_duration INTEGER DEFAULT 60000 NOT NULL,
    lease_id       VARCHAR               NOT NULL
        CONSTRAINT lease_pk
            PRIMARY KEY
);

COMMENT ON COLUMN edc_lease.leased_at IS 'posix timestamp of lease';

COMMENT ON COLUMN edc_lease.lease_duration IS 'duration of lease in milliseconds';


CREATE UNIQUE INDEX IF NOT EXISTS lease_lease_id_uindex
    ON edc_lease (lease_id);

CREATE TABLE IF NOT EXISTS edc_edr_cache
(
    transfer_process_id           VARCHAR NOT NULL PRIMARY KEY,
    agreement_id                  VARCHAR NOT NULL,
    asset_id                      VARCHAR NOT NULL,
    edr_id                        VARCHAR NOT NULL,
    contract_negotiation_id       VARCHAR,
    provider_id                   VARCHAR,
    expiration_timestamp          BIGINT,
    state                         INTEGER DEFAULT 0                                  NOT NULL,
    state_count                   INTEGER DEFAULT 0,
    state_timestamp               BIGINT,
    error_detail                  VARCHAR,
    lease_id                      VARCHAR CONSTRAINT edc_edr_cache_lease_lease_id_fk REFERENCES edc_lease ON DELETE SET NULL,
    created_at                    BIGINT  NOT NULL,
    updated_at                    BIGINT  NOT NULL
);

CREATE INDEX IF NOT EXISTS edc_edr_asset_id_index
    ON edc_edr_cache (asset_id);


CREATE INDEX IF NOT EXISTS edc_edr_agreement_id_index
    ON edc_edr_cache (agreement_id);
