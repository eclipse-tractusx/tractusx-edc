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

CREATE TABLE IF NOT EXISTS edc_edr_cache
(
    transfer_process_id           VARCHAR NOT NULL PRIMARY KEY,
    agreement_id                  VARCHAR NOT NULL,
    asset_id                      VARCHAR NOT NULL,
    edr_id                        VARCHAR NOT NULL,
    provider_id                   VARCHAR,
    created_at                    BIGINT  NOT NULL,
    updated_at                    BIGINT  NOT NULL
);
