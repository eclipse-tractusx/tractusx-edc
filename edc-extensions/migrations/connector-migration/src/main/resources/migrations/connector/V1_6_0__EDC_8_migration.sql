CREATE INDEX IF NOT EXISTS data_plane_state ON edc_data_plane (state,state_time_stamp);

ALTER TABLE edc_data_plane ADD COLUMN IF NOT EXISTS transfer_type_destination VARCHAR DEFAULT 'HttpData';

CREATE INDEX IF NOT EXISTS transfer_process_state ON edc_transfer_process (state,state_time_stamp);

CREATE INDEX IF NOT EXISTS policy_monitor_state ON edc_policy_monitor (state,state_time_stamp);

CREATE INDEX IF NOT EXISTS contract_negotiation_state ON edc_contract_negotiation (state,state_timestamp);

CREATE TABLE IF NOT EXISTS edc_federated_catalog
(
    id                    VARCHAR PRIMARY KEY NOT NULL,
    catalog               JSON,
    marked                BOOLEAN DEFAULT FALSE
);

ALTER TABLE edc_policydefinitions ADD COLUMN IF NOT EXISTS profiles JSON;

CREATE TABLE IF NOT EXISTS edc_data_plane_instance
(
    id                   VARCHAR NOT NULL PRIMARY KEY,
    data                 JSON,
    lease_id             VARCHAR
    CONSTRAINT data_plane_instance_lease_id_fk
    REFERENCES edc_lease
    ON DELETE SET NULL
);


CREATE TABLE IF NOT EXISTS edc_lease
(
    leased_by      VARCHAR NOT NULL,
    leased_at      BIGINT,
    lease_duration INTEGER NOT NULL,
    lease_id       VARCHAR NOT NULL
    CONSTRAINT lease_pk
    PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS edc_agreement_retirement
(
    contract_agreement_id     VARCHAR PRIMARY KEY,
    reason                    TEXT   NOT NULL,
    agreement_retirement_date BIGINT NOT NULL
);
















