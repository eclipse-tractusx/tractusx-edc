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

-- convert types to JSON
ALTER TABLE edc_policies ALTER COLUMN permissions TYPE JSON USING permissions::json;
ALTER TABLE edc_policies ALTER COLUMN prohibitions TYPE JSON USING prohibitions::json;
ALTER TABLE edc_policies ALTER COLUMN duties TYPE JSON USING duties::json;
ALTER TABLE edc_policies ALTER COLUMN extensible_properties TYPE JSON USING extensible_properties::json;

-- rename policy table
ALTER TABLE edc_policies RENAME TO edc_policydefinitions;
