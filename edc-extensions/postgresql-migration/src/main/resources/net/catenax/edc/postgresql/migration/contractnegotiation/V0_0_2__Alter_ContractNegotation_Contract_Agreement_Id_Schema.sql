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
--       Mercedes-Benz Tech Innovation GmbH - Migrate edc_contract_negotiation to contain agreement_id
--

-- Add new agreement_id
ALTER TABLE edc_contract_agreement ADD agreement_id VARCHAR(255) DEFAULT NULL;
ALTER TABLE edc_contract_negotiation ALTER COLUMN correlation_id DROP NOT NULL;