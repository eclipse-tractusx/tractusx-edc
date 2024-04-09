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
--       Mercedes-Benz Tech Innovation GmbH - Add DataAddress Column
--


-- Add new content_data_address columns
ALTER TABLE edc_transfer_process ADD content_data_address TEXT;
COMMENT ON COLUMN edc_transfer_process.content_data_address IS 'DataAddress serialized as JSON';
