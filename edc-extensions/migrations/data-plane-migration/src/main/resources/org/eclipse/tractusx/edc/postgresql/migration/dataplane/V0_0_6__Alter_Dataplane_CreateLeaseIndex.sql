--
--  Copyright (c) 2025 SAP SE
--
--  This program and the accompanying materials are made available under the
--  terms of the Apache License, Version 2.0 which is available at
--  https://www.apache.org/licenses/LICENSE-2.0
--
--  SPDX-License-Identifier: Apache-2.0
--
--  Contributors:
--       SAP SE - add index for foreign keys
--

CREATE INDEX IF NOT EXISTS data_plane_lease_id ON edc_data_plane (lease_id);