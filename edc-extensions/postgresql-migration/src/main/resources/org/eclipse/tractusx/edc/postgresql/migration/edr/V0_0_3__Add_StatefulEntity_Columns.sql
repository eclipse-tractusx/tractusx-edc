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


-- add column
ALTER TABLE edc_edr_cache ADD COLUMN expiration_timestamp BIGINT;
ALTER TABLE edc_edr_cache ADD COLUMN state INTEGER DEFAULT 50 NOT NULL;
ALTER TABLE edc_edr_cache ADD COLUMN state_count INTEGER DEFAULT 0;
ALTER TABLE edc_edr_cache ADD COLUMN state_timestamp BIGINT;
ALTER TABLE edc_edr_cache ADD COLUMN error_detail VARCHAR;
ALTER TABLE edc_edr_cache ADD COLUMN lease_id VARCHAR CONSTRAINT edc_edr_cache_lease_lease_id_fk REFERENCES edc_lease ON DELETE SET NULL;

