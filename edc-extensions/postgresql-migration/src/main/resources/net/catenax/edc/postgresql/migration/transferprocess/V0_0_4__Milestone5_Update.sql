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
--       Mercedes-Benz Tech Innovation GmbH - EDC Milestone 5 Update
--

-- add columns
ALTER TABLE edc_transfer_process ADD COLUMN created_time_stamp BIGINT;
ALTER TABLE edc_transfer_process ADD COLUMN deprovisioned_resources JSON;

COMMENT ON COLUMN edc_transfer_process.deprovisioned_resources IS 'List of deprovisioned resources, serialized as JSON';

-- convert types to JSON
ALTER TABLE edc_transfer_process ALTER COLUMN provisioned_resource_set TYPE JSON USING provisioned_resource_set::json;
ALTER TABLE edc_transfer_process ALTER COLUMN trace_context TYPE JSON USING trace_context::json;
ALTER TABLE edc_transfer_process ALTER COLUMN resource_manifest TYPE JSON USING resource_manifest::json;
ALTER TABLE edc_transfer_process ALTER COLUMN content_data_address TYPE JSON USING content_data_address::json;
