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
--       Mercedes-Benz Tech Innovation GmbH - Rewrite to be SQL Init Schema
--

--
-- table: edc_lease
--
CREATE TABLE IF NOT EXISTS edc_lease
(
    leased_by      VARCHAR(255)          NOT NULL,
    leased_at      BIGINT,
    lease_duration INTEGER DEFAULT 60000 NOT NULL,
    lease_id       VARCHAR(255)          NOT NULL
        CONSTRAINT lease_pk
            PRIMARY KEY
);
COMMENT ON COLUMN edc_lease.leased_at IS 'posix timestamp of lease';
COMMENT ON COLUMN edc_lease.lease_duration IS 'duration of lease in milliseconds';

CREATE UNIQUE INDEX IF NOT EXISTS lease_lease_id_uindex
    ON edc_lease (lease_id);

--
-- table: edc_transfer_process
--
CREATE TABLE IF NOT EXISTS edc_transfer_process
(
    id                       VARCHAR(255)      NOT NULL
        CONSTRAINT transfer_process_pk
            PRIMARY KEY,
    type                     VARCHAR(255)      NOT NULL,
    state                    INTEGER           NOT NULL,
    state_count              INTEGER DEFAULT 0 NOT NULL,
    state_time_stamp         BIGINT,
    trace_context            TEXT,
    error_detail             TEXT,
    resource_manifest        TEXT,
    provisioned_resource_set TEXT,
    lease_id                 VARCHAR(255)
        CONSTRAINT transfer_process_lease_lease_id_fk
            REFERENCES edc_lease
            ON DELETE SET NULL
);
COMMENT ON COLUMN edc_transfer_process.trace_context IS 'Java Map serialized as JSON';
COMMENT ON COLUMN edc_transfer_process.resource_manifest IS 'java ResourceManifest serialized as JSON';
COMMENT ON COLUMN edc_transfer_process.provisioned_resource_set IS 'ProvisionedResourceSet serialized as JSON';

CREATE UNIQUE INDEX IF NOT EXISTS transfer_process_id_uindex
    ON edc_transfer_process (id);

--
-- table: edc_data_request
--
CREATE TABLE IF NOT EXISTS edc_data_request
(
    id                  VARCHAR(255) NOT NULL
        CONSTRAINT data_request_pk
            PRIMARY KEY,
    process_id          VARCHAR(255) NOT NULL,
    connector_address   VARCHAR(255) NOT NULL,
    protocol            VARCHAR(255) NOT NULL,
    connector_id        VARCHAR(255),
    asset_id            VARCHAR(255) NOT NULL,
    contract_id         VARCHAR(255) NOT NULL,
    data_destination    TEXT         NOT NULL,
    managed_resources   BOOLEAN DEFAULT TRUE,
    properties          TEXT,
    transfer_type       TEXT,
    transfer_process_id VARCHAR(255) NOT NULL
        CONSTRAINT data_request_transfer_process_id_fk
            REFERENCES edc_transfer_process
            ON UPDATE RESTRICT ON DELETE CASCADE
);
COMMENT ON COLUMN edc_data_request.data_destination IS 'DataAddress serialized as JSON';
COMMENT ON COLUMN edc_data_request.properties IS 'java Map serialized as JSON';
COMMENT ON COLUMN edc_data_request.transfer_type IS 'TransferType serialized as JSON';

CREATE UNIQUE INDEX IF NOT EXISTS data_request_id_uindex
    ON edc_data_request (id);
CREATE UNIQUE INDEX IF NOT EXISTS lease_lease_id_uindex
    ON edc_lease (lease_id);

