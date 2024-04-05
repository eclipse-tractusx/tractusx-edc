--
--  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft
--
--  This program and the accompanying materials are made available under the
--  terms of the Apache License, Version 2.0 which is available at
--  https://www.apache.org/licenses/LICENSE-2.0
--
--  SPDX-License-Identifier: Apache-2.0
--
--  Contributors:
--       Bayerische Motoren Werke Aktiengesellschaft - Migration into one table
--

UPDATE edc_asset SET
properties = (select json_object_agg(json_array_elements->>'property_name', json_array_elements->>'property_value') from json_array_elements(properties) as json_array_elements),
private_properties = (select json_object_agg(json_array_elements->>'property_name', json_array_elements->>'property_value') from json_array_elements(private_properties) as json_array_elements)
