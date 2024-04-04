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
--       Mercedes-Benz Tech Innovation GmbH - EDC Snapshot 20221201 Update
--

-- add columns
ALTER TABLE edc_contract_definitions ADD COLUMN validity BIGINT;
-- set to 60×60×24×365, to let existing contracts expire after one year
UPDATE edc_contract_definitions SET validity=31536000;
ALTER TABLE edc_contract_definitions ALTER COLUMN validity SET NOT NULL;
