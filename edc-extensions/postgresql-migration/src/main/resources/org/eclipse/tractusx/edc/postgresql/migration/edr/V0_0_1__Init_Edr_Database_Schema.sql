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
--       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
--

-- Statements are designed for and tested with Postgres only!


CREATE TABLE IF NOT EXISTS edc_edr_cache
(
    transfer_process_id           VARCHAR NOT NULL PRIMARY KEY,
    agreement_id                  VARCHAR NOT NULL,
    asset_id                      VARCHAR NOT NULL,
    edr_id                        VARCHAR NOT NULL,
    created_at                    BIGINT  NOT NULL,
    updated_at                    BIGINT  NOT NULL
);


CREATE INDEX IF NOT EXISTS edc_edr_asset_id_index
    ON edc_edr_cache (asset_id);


CREATE INDEX IF NOT EXISTS edc_edr_agreement_id_index
    ON edc_edr_cache (agreement_id);

