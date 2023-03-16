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
--       Mercedes-Benz Tech Innovation GmbH - Migrate edc_contract_definitions to contain access_policy_id and contract_policy_id
--

-- Add new access_policy_id and contract_policy_id columns
ALTER TABLE edc_contract_definitions ADD access_policy_id VARCHAR(255) DEFAULT NULL;
ALTER TABLE edc_contract_definitions ADD contract_policy_id VARCHAR(255) DEFAULT NULL;

-- Extract the id from access_policy and store its value into access_policy_id
UPDATE edc_contract_definitions SET access_policy_id=access_policy::json->>'uid';
-- Extract the id from contract_policy and store its value into contract_policy_id
UPDATE edc_contract_definitions SET contract_policy_id=contract_policy::json->>'uid';

-- DROP obsolete access_policy columns
ALTER TABLE edc_contract_definitions DROP COLUMN access_policy;
ALTER TABLE edc_contract_definitions DROP COLUMN contract_policy;

-- Add non-null constraints to the new columns
ALTER TABLE edc_contract_definitions ALTER COLUMN access_policy_id SET NOT NULL;
ALTER TABLE edc_contract_definitions ALTER COLUMN contract_policy_id SET NOT NULL;
