CREATE TABLE IF NOT EXISTS edc_contract_agreement_bpns
(
    agreement_id VARCHAR
    CONSTRAINT contract_agreement_bpns_contract_agreement_id_fk PRIMARY KEY
    REFERENCES edc_contract_agreement,
    provider_bpn VARCHAR(255) NOT NULL,
    consumer_bpn VARCHAR(255) NOT NULL
    );

CREATE TABLE IF NOT EXISTS edc_jti_validation
(
    token_id   VARCHAR NOT NULL PRIMARY KEY,
    expires_at BIGINT
);

CREATE INDEX IF NOT EXISTS contract_negotiation_lease_id_index
    ON edc_contract_negotiation (lease_id);

CREATE INDEX IF NOT EXISTS contract_negotiation_agreement_id_index
    ON edc_contract_negotiation (agreement_id);

CREATE INDEX IF NOT EXISTS policy_monitor_lease_id_index
    ON edc_policy_monitor (lease_id);

CREATE INDEX IF NOT EXISTS transfer_process_lease_id_index
    ON edc_transfer_process (lease_id);


ALTER TABLE edc_data_plane
    ADD COLUMN IF NOT EXISTS resource_definitions json DEFAULT '[]'::json;

CREATE INDEX IF NOT EXISTS data_plane_lease_id ON edc_data_plane (lease_id);
