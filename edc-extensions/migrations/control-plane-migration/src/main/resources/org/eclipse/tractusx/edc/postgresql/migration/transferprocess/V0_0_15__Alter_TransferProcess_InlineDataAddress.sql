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
--       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
--

-- drop datarequest
DROP TABLE IF EXISTS edc_data_request;
-- add columns
ALTER TABLE edc_transfer_process
    ADD COLUMN correlation_id VARCHAR;
ALTER TABLE edc_transfer_process
    ADD COLUMN counter_party_address VARCHAR;
ALTER TABLE edc_transfer_process
    ADD COLUMN protocol VARCHAR;
ALTER TABLE edc_transfer_process
    ADD COLUMN asset_id VARCHAR;
ALTER TABLE edc_transfer_process
    ADD COLUMN contract_id VARCHAR;
ALTER TABLE edc_transfer_process
    ADD COLUMN data_destination JSON;
