--
--  Copyright (c) 2026 Cofinity-X GmbH
--
--  This program and the accompanying materials are made available under the
--  terms of the Apache License, Version 2.0 which is available at
--  https://www.apache.org/licenses/LICENSE-2.0
--
--  SPDX-License-Identifier: Apache-2.0
--
--  Contributors:
--       Cofinity-X GmbH - initial API and implementation
--
--
-- table: edc_contract_agreement, edc_transfer_process
--

ALTER TABLE edc_contract_agreement ADD COLUMN IF NOT EXISTS claims JSON;

ALTER TABLE edc_transfer_process ADD COLUMN IF NOT EXISTS claims JSON;