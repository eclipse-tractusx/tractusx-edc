CREATE TABLE edc_agreement_retirement (
    contract_agreement_id character varying NOT NULL,
    reason text NOT NULL,
    agreement_retirement_date bigint NOT NULL
);

CREATE TABLE edc_asset (
    asset_id character varying(255) NOT NULL,
    created_at bigint,
    properties json,
    private_properties json,
    data_address json
);

CREATE TABLE edc_business_partner_group (
    bpn character varying NOT NULL,
    groups json DEFAULT '[]'::json NOT NULL
);

CREATE TABLE edc_contract_agreement (
    agr_id character varying NOT NULL,
    provider_agent_id character varying(255),
    consumer_agent_id character varying(255),
    signing_date bigint,
    start_date bigint,
    end_date integer,
    asset_id character varying(255) NOT NULL,
    policy_id character varying(255),
    serialized_policy text,
    policy json
);

CREATE TABLE edc_contract_agreement_bpns (
    agreement_id character varying NOT NULL,
    provider_bpn character varying(255) NOT NULL,
    consumer_bpn character varying(255) NOT NULL
);

CREATE TABLE edc_contract_definitions (
    contract_definition_id character varying(255) NOT NULL,
    assets_selector json NOT NULL,
    access_policy_id character varying(255) DEFAULT NULL::character varying NOT NULL,
    contract_policy_id character varying(255) DEFAULT NULL::character varying NOT NULL,
    created_at bigint,
    private_properties json
);

CREATE TABLE edc_contract_negotiation (
    id character varying(255) NOT NULL,
    correlation_id character varying(255),
    counterparty_id character varying(255) NOT NULL,
    counterparty_address character varying(255) NOT NULL,
    protocol character varying(255) DEFAULT 'ids-multipart'::character varying NOT NULL,
    type character varying DEFAULT 0 NOT NULL,
    state integer DEFAULT 0 NOT NULL,
    state_count integer DEFAULT 0,
    state_timestamp bigint,
    error_detail text,
    agreement_id text,
    contract_offers json,
    trace_context json,
    lease_id character varying(255),
    created_at bigint,
    updated_at bigint,
    callback_addresses json,
    pending boolean DEFAULT false,
    protocol_messages json DEFAULT '{}'::json
);

CREATE TABLE edc_data_plane_instance (
    id character varying NOT NULL,
    data json,
    lease_id character varying
);

CREATE TABLE edc_edr_entry (
    transfer_process_id character varying NOT NULL,
    agreement_id character varying NOT NULL,
    asset_id character varying NOT NULL,
    provider_id character varying NOT NULL,
    contract_negotiation_id character varying,
    created_at bigint NOT NULL
);

CREATE TABLE edc_federated_catalog (
    id character varying NOT NULL,
    catalog json,
    marked boolean DEFAULT false
);

CREATE TABLE edc_jti_validation (
    token_id character varying NOT NULL,
    expires_at bigint
);

CREATE TABLE edc_lease (
    leased_by character varying(255) NOT NULL,
    leased_at bigint,
    lease_duration integer DEFAULT 60000 NOT NULL,
    lease_id character varying(255) NOT NULL
);

CREATE TABLE edc_policy_monitor (
    entry_id character varying NOT NULL,
    state integer NOT NULL,
    created_at bigint NOT NULL,
    updated_at bigint NOT NULL,
    state_count integer DEFAULT 0 NOT NULL,
    state_time_stamp bigint,
    trace_context json,
    error_detail character varying,
    lease_id character varying,
    properties json,
    contract_id character varying
);

CREATE TABLE edc_policydefinitions (
    policy_id character varying(255) NOT NULL,
    permissions json,
    prohibitions json,
    duties json,
    extensible_properties json,
    inherits_from character varying(255),
    assigner character varying(255),
    assignee character varying(255),
    target character varying(255),
    policy_type character varying(255) NOT NULL,
    created_at bigint,
    private_properties json,
    profiles json
);

CREATE TABLE edc_transfer_process (
    transferprocess_id character varying(255) NOT NULL,
    type character varying(255) NOT NULL,
    state integer NOT NULL,
    state_count integer DEFAULT 0 NOT NULL,
    state_time_stamp bigint,
    trace_context json,
    error_detail text,
    resource_manifest json,
    provisioned_resource_set json,
    lease_id character varying(255),
    content_data_address json,
    created_at bigint,
    deprovisioned_resources json,
    updated_at bigint,
    private_properties text DEFAULT '{}'::text,
    callback_addresses json,
    pending boolean DEFAULT false,
    transfer_type character varying,
    protocol_messages json DEFAULT '{}'::json,
    data_plane_id character varying,
    correlation_id character varying,
    counter_party_address character varying,
    protocol character varying,
    asset_id character varying,
    contract_id character varying,
    data_destination json
);

ALTER TABLE ONLY edc_contract_agreement_bpns
    ADD CONSTRAINT contract_agreement_bpns_contract_agreement_id_fk PRIMARY KEY (agreement_id);

ALTER TABLE ONLY edc_contract_agreement
    ADD CONSTRAINT contract_agreement_pk PRIMARY KEY (agr_id);

ALTER TABLE ONLY edc_contract_negotiation
    ADD CONSTRAINT contract_negotiation_pk PRIMARY KEY (id);

ALTER TABLE ONLY edc_agreement_retirement
    ADD CONSTRAINT edc_agreement_retirement_pkey PRIMARY KEY (contract_agreement_id);

ALTER TABLE ONLY edc_asset
    ADD CONSTRAINT edc_asset_pkey PRIMARY KEY (asset_id);

ALTER TABLE ONLY edc_business_partner_group
    ADD CONSTRAINT edc_business_partner_group_pk PRIMARY KEY (bpn);

ALTER TABLE ONLY edc_contract_definitions
    ADD CONSTRAINT edc_contract_definitions_pkey PRIMARY KEY (contract_definition_id);

ALTER TABLE ONLY edc_data_plane_instance
    ADD CONSTRAINT edc_data_plane_instance_pkey PRIMARY KEY (id);

ALTER TABLE ONLY edc_edr_entry
    ADD CONSTRAINT edc_edr_entry_pkey PRIMARY KEY (transfer_process_id);

ALTER TABLE ONLY edc_federated_catalog
    ADD CONSTRAINT edc_federated_catalog_pkey PRIMARY KEY (id);

ALTER TABLE ONLY edc_jti_validation
    ADD CONSTRAINT edc_jti_validation_pkey PRIMARY KEY (token_id);

ALTER TABLE ONLY edc_policydefinitions
    ADD CONSTRAINT edc_policies_pkey PRIMARY KEY (policy_id);

ALTER TABLE ONLY edc_policy_monitor
    ADD CONSTRAINT edc_policy_monitor_pkey PRIMARY KEY (entry_id);

ALTER TABLE ONLY edc_lease
    ADD CONSTRAINT lease_pk PRIMARY KEY (lease_id);

ALTER TABLE ONLY edc_transfer_process
    ADD CONSTRAINT transfer_process_pk PRIMARY KEY (transferprocess_id);

CREATE INDEX asset_id_index ON edc_edr_entry USING btree (asset_id);

CREATE UNIQUE INDEX contract_agreement_id_uindex ON edc_contract_agreement USING btree (agr_id);

CREATE INDEX contract_negotiation_agreement_id_index ON edc_contract_negotiation USING btree (agreement_id);

CREATE INDEX contract_negotiation_correlationid_index ON edc_contract_negotiation USING btree (correlation_id);

CREATE UNIQUE INDEX contract_negotiation_id_uindex ON edc_contract_negotiation USING btree (id);

CREATE INDEX contract_negotiation_lease_id_index ON edc_contract_negotiation USING btree (lease_id);

CREATE INDEX contract_negotiation_state ON edc_contract_negotiation USING btree (state, state_timestamp);

CREATE UNIQUE INDEX edc_policies_id_uindex ON edc_policydefinitions USING btree (policy_id);

CREATE UNIQUE INDEX lease_lease_id_uindex ON edc_lease USING btree (lease_id);

CREATE INDEX policy_monitor_lease_id_index ON edc_policy_monitor USING btree (lease_id);

CREATE INDEX policy_monitor_state ON edc_policy_monitor USING btree (state, state_time_stamp);

CREATE UNIQUE INDEX transfer_process_id_uindex ON edc_transfer_process USING btree (transferprocess_id);

CREATE INDEX transfer_process_lease_id_index ON edc_transfer_process USING btree (lease_id);

CREATE INDEX transfer_process_state ON edc_transfer_process USING btree (state, state_time_stamp);

ALTER TABLE ONLY edc_contract_negotiation
    ADD CONSTRAINT contract_negotiation_contract_agreement_id_fk FOREIGN KEY (agreement_id) REFERENCES edc_contract_agreement(agr_id);

ALTER TABLE ONLY edc_contract_negotiation
    ADD CONSTRAINT contract_negotiation_lease_lease_id_fk FOREIGN KEY (lease_id) REFERENCES edc_lease(lease_id) ON DELETE SET NULL;

ALTER TABLE ONLY edc_data_plane_instance
    ADD CONSTRAINT data_plane_instance_lease_id_fk FOREIGN KEY (lease_id) REFERENCES edc_lease(lease_id) ON DELETE SET NULL;

ALTER TABLE ONLY edc_contract_agreement_bpns
    ADD CONSTRAINT edc_contract_agreement_bpns_agreement_id_fkey FOREIGN KEY (agreement_id) REFERENCES edc_contract_agreement(agr_id);

ALTER TABLE ONLY edc_policy_monitor
    ADD CONSTRAINT policy_monitor_lease_lease_id_fk FOREIGN KEY (lease_id) REFERENCES edc_lease(lease_id) ON DELETE SET NULL;

ALTER TABLE ONLY edc_transfer_process
    ADD CONSTRAINT transfer_process_lease_lease_id_fk FOREIGN KEY (lease_id) REFERENCES edc_lease(lease_id) ON DELETE SET NULL;


CREATE TABLE edc_accesstokendata (
    id character varying NOT NULL,
    claim_token json NOT NULL,
    data_address json NOT NULL,
    additional_properties json DEFAULT '{}'::json
);

CREATE TABLE edc_data_plane (
    process_id character varying NOT NULL,
    state integer NOT NULL,
    created_at bigint NOT NULL,
    updated_at bigint NOT NULL,
    state_count integer DEFAULT 0 NOT NULL,
    state_time_stamp bigint,
    trace_context json,
    error_detail character varying,
    callback_address character varying,
    lease_id character varying,
    source json,
    destination json,
    properties json,
    flow_type character varying,
    transfer_type_destination character varying DEFAULT 'HttpData'::character varying,
    runtime_id character varying,
    resource_definitions json DEFAULT '[]'::json
);

ALTER TABLE ONLY edc_accesstokendata
    ADD CONSTRAINT edc_accesstokendata_pkey PRIMARY KEY (id);

ALTER TABLE ONLY edc_data_plane
    ADD CONSTRAINT edc_data_plane_pkey PRIMARY KEY (process_id);

CREATE INDEX data_plane_lease_id ON edc_data_plane USING btree (lease_id);

CREATE INDEX data_plane_state ON edc_data_plane USING btree (state, state_time_stamp);

ALTER TABLE ONLY edc_data_plane
    ADD CONSTRAINT data_plane_lease_lease_id_fk FOREIGN KEY (lease_id) REFERENCES edc_lease(lease_id) ON DELETE SET NULL;
