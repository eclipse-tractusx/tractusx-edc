--
--  Copyright (c) 2022 ZF Friedrichshafen AG
--
--  This program and the accompanying materials are made available under the
--  terms of the Apache License, Version 2.0 which is available at
--  https://www.apache.org/licenses/LICENSE-2.0
--
--  SPDX-License-Identifier: Apache-2.0
--
--  Contributors:
--       ZF Friedrichshafen AG - Add column for the dynamically HTTP Receiver callback endpoints
--

-- add column
ALTER TABLE edc_transfer_process ADD COLUMN transferprocess_properties TEXT;
