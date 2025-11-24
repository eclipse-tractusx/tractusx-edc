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

CREATE TABLE edc_lease (
    leased_by character varying NOT NULL,
    leased_at bigint,
    lease_duration integer NOT NULL,
    lease_id character varying NOT NULL
);

ALTER TABLE ONLY edc_accesstokendata
    ADD CONSTRAINT edc_accesstokendata_pkey PRIMARY KEY (id);

ALTER TABLE ONLY edc_data_plane
    ADD CONSTRAINT edc_data_plane_pkey PRIMARY KEY (process_id);

ALTER TABLE ONLY edc_lease
    ADD CONSTRAINT lease_pk PRIMARY KEY (lease_id);

CREATE INDEX data_plane_lease_id ON edc_data_plane USING btree (lease_id);

CREATE INDEX data_plane_state ON edc_data_plane USING btree (state, state_time_stamp);

ALTER TABLE ONLY edc_data_plane
    ADD CONSTRAINT data_plane_lease_lease_id_fk FOREIGN KEY (lease_id) REFERENCES edc_lease(lease_id) ON DELETE SET NULL;
