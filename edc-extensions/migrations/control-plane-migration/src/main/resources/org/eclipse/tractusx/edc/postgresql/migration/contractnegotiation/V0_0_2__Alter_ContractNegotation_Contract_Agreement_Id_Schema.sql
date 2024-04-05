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
--       Mercedes-Benz Tech Innovation GmbH -  contract agreement id column rename, negotiation correlation id nullable
--

-- RENAME id column and make correlation_id nullable
ALTER TABLE IF EXISTS edc_contract_agreement RENAME COLUMN id to agreement_id;
ALTER TABLE IF EXISTS edc_contract_negotiation ALTER COLUMN correlation_id DROP NOT NULL;
