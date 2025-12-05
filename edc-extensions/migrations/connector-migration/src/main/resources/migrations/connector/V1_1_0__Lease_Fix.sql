ALTER TABLE edc_contract_negotiation DROP CONSTRAINT contract_negotiation_lease_lease_id_fk;
ALTER TABLE edc_data_plane DROP CONSTRAINT data_plane_lease_lease_id_fk;
ALTER TABLE edc_data_plane_instance DROP CONSTRAINT data_plane_instance_lease_id_fk;
ALTER TABLE edc_policy_monitor DROP CONSTRAINT policy_monitor_lease_lease_id_fk;
ALTER TABLE edc_transfer_process DROP CONSTRAINT transfer_process_lease_lease_id_fk;

DELETE FROM edc_lease WHERE lease_id IS NULL;

ALTER TABLE edc_lease
    ADD COLUMN resource_id varchar NOT NULL,
    ADD COLUMN resource_kind varchar NOT NULL,
    DROP CONSTRAINT lease_pk,
    ALTER lease_id DROP NOT NULL,
    ADD CONSTRAINT lease_pk PRIMARY KEY (resource_id, resource_kind);
