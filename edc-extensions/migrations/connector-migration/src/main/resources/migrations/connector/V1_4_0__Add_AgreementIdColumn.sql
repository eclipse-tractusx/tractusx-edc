ALTER TABLE edc_contract_agreement ADD COLUMN agr_agreement_id varchar;

UPDATE edc_contract_agreement SET agr_agreement_id = agr_id WHERE agr_agreement_id IS NULL;

ALTER TABLE edc_contract_agreement ALTER COLUMN agr_agreement_id SET NOT NULL;

