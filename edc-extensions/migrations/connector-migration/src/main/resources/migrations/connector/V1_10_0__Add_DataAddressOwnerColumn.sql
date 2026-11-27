--
--  Copyright (c) 2026 Contributors to the Eclipse Foundation
--
--  This program and the accompanying materials are made available under the
--  terms of the Apache License, Version 2.0 which is available at
--  https://www.apache.org/licenses/LICENSE-2.0
--
--  SPDX-License-Identifier: Apache-2.0
--  Assisted-By: Claude Code (claude-opus-5-5)
--
--  Contributors:
--       Contributors to the Eclipse Foundation - initial API and implementation
--
--
-- table: edc_transfer_process
--
-- Upstream EDC 0.18 added the column data_address_owner: it is TRUE when the data address of the transfer was produced
-- by the local data plane (on prepare or start), and it decides how a SUSPENDED transfer is resumed (RESUMING vs.
-- RESUMING_REQUESTED). With the legacy data plane signaling used by Tractus-X, the only transfers for which upstream
-- sets the flag are provider-side PULL transfers, whose data plane returns the EDR on start. Those are backfilled, so
-- that transfers created before the upgrade can still be resumed by the provider as in EDC 0.17.
--

ALTER TABLE edc_transfer_process ADD COLUMN IF NOT EXISTS data_address_owner BOOLEAN DEFAULT FALSE;

UPDATE edc_transfer_process
SET data_address_owner = TRUE
WHERE type = 'PROVIDER'
  AND transfer_type LIKE '%-PULL'
  AND data_address_owner = FALSE;
