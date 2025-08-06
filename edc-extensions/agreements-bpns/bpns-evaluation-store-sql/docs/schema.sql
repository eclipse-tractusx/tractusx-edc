--
--  Copyright (c) 2025 Cofinity-X GmbH
--
--  This program and the accompanying materials are made available under the
--  terms of the Apache License, Version 2.0 which is available at
--  https://www.apache.org/licenses/LICENSE-2.0
--
--  SPDX-License-Identifier: Apache-2.0
--
--  Contributors:
--       Cofinity-X GmbH
--

--
-- table: edc_contract_agreement_bpns
--
CREATE TABLE IF NOT EXISTS edc_contract_agreement_bpns
(
    agreement_id VARCHAR,
    provider_bpn VARCHAR(255),
    consumer_bpn VARCHAR(255)
);