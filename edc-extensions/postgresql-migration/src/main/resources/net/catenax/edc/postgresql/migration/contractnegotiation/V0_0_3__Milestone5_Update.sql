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

-- rename id columns
ALTER TABLE edc_contract_negotiation RENAME COLUMN contract_agreement_id TO agreement_id;
ALTER TABLE edc_contract_agreement RENAME COLUMN agreement_id TO agr_id;

-- convert types to JSON
ALTER TABLE edc_contract_negotiation ALTER COLUMN contract_offers TYPE JSON USING contract_offers::json;
ALTER TABLE edc_contract_negotiation ALTER COLUMN trace_context TYPE JSON USING trace_context::json;

-- new policy column
ALTER TABLE edc_contract_agreement ADD COLUMN policy JSON;
