--
--  Copyright (c) 2024 Contributors to the Eclipse Foundation
--
--  This program and the accompanying materials are made available under the
--  terms of the Apache License, Version 2.0 which is available at
--  https://www.apache.org/licenses/LICENSE-2.0
--
--  SPDX-License-Identifier: Apache-2.0
--
--  Contributors:
--       Contributors to the Eclipse Foundation - initial API and implementation
--

-- add columns
ALTER TABLE edc_policydefinitions ADD COLUMN profiles JSON;
