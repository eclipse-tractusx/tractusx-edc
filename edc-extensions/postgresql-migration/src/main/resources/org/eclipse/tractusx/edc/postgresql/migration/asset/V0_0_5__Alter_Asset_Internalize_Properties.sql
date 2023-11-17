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
--       Mercedes-Benz Tech Innovation GmbH - EDC Snapshot 20220815 Update
--

-- add columns
ALTER TABLE edc_asset
    ADD COLUMN properties JSON;

ALTER TABLE edc_asset
    ADD COLUMN private_properties JSON;

ALTER TABLE edc_asset
    ADD COLUMN data_address JSON;

-- update data address, move all JSON data into the edc_asset table
UPDATE edc_asset
SET data_address = (SELECT properties FROM edc_asset_dataaddress WHERE asset_id_fk = a.asset_id)::json
FROM edc_asset as a
WHERE edc_asset.asset_id = a.asset_id;


-- update properties, move all JSON data from the edc_asset_properties table
UPDATE edc_asset
SET private_properties = (SELECT json_agg(json_build_object('property_name', prop.property_name, 'property_value',
                                                            prop.property_value, 'property_type', prop.property_type))
                          FROM edc_asset_property prop
                          WHERE asset_id_fk = a.asset_id
                            AND prop.property_is_private = true)
FROM edc_asset as a
WHERE edc_asset.asset_id = a.asset_id;

-- update private properties, move all JSON data from the edc_asset_properties table
UPDATE edc_asset
SET properties = (SELECT json_agg(json_build_object('property_name', prop.property_name, 'property_value',
                                                    prop.property_value, 'property_type', prop.property_type))
                  FROM edc_asset_property prop
                  WHERE asset_id_fk = a.asset_id
                    AND prop.property_is_private = false)
FROM edc_asset as a
WHERE edc_asset.asset_id = a.asset_id;


-- do NOT drop edc_asset_dataaddress and edc_asset_property to enable further data migration scripts


