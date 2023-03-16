--
--  Copyright (c) 2022 ZF Friedrichshafen AG
--
--  This program and the accompanying materials are made available under the
--  terms of the Apache License, Version 2.0 which is available at
--  https://www.apache.org/licenses/LICENSE-2.0
--
--  SPDX-License-Identifier: Apache-2.0
--
--  Contributors:
--       ZF Friedrichshafen AG - Initial SQL Query
--

-- Statements are designed for and tested with Postgres only!


CREATE TABLE IF NOT EXISTS edc_lease
(
    leased_by      VARCHAR NOT NULL,
    leased_at      BIGINT,
    lease_duration INTEGER NOT NULL,
    lease_id       VARCHAR NOT NULL
        CONSTRAINT lease_pk
            PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS edc_cpadapter_queue
(
    id                   VARCHAR NOT NULL,
    created_at           BIGINT  NOT NULL,
    channel              VARCHAR,
    message              JSON,
    invoke_after         BIGINT  NOT NULL,
    lease_id             VARCHAR
                         CONSTRAINT cpadapter_queue_lease_lease_id_fk
                         REFERENCES edc_lease
                         ON DELETE SET NULL,
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS edc_cpadapter_queue_id_uindex
    ON edc_cpadapter_queue (id);

CREATE TABLE IF NOT EXISTS edc_cpadapter_object_store
(
    id                   VARCHAR NOT NULL,
    created_at           BIGINT  NOT NULL,
    type                 VARCHAR,
    object               JSON,
    PRIMARY KEY (id)
);



