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

-- rename asset id
ALTER TABLE edc_asset_dataaddress RENAME COLUMN asset_id TO asset_id_fk;
ALTER TABLE edc_asset_property RENAME COLUMN asset_id TO asset_id_fk;
