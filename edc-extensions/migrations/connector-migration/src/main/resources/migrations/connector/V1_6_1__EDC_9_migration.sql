UPDATE edc_policydefinitions SET profiles='[]'::json where profiles is NULL;

ALTER TABLE edc_data_plane
    ADD COLUMN IF NOT EXISTS runtime_id VARCHAR;
