--
--  Copyright (c) 2022 ZF Friedrichshafen AG and others
--
--  This program and the accompanying materials are made available under the
--  terms of the Apache License, Version 2.0 which is available at
--  https://www.apache.org/licenses/LICENSE-2.0
--
--  SPDX-License-Identifier: Apache-2.0
--
--  Contributors:
--       ZF Friedrichshafen AG - Initial SQL Query
--       Daimler TSS GmbH - Value range modifications
--

--
-- table: edc_policies
--
CREATE TABLE IF NOT EXISTS edc_policies
(
    policy_id             VARCHAR(255) NOT NULL,
    permissions           TEXT,
    prohibitions          TEXT,
    duties                TEXT,
    extensible_properties TEXT,
    inherits_from         VARCHAR(255),
    assigner              VARCHAR(255),
    assignee              VARCHAR(255),
    target                VARCHAR(255),
    policy_type           VARCHAR(255) NOT NULL,
    PRIMARY KEY (policy_id)
);

COMMENT ON COLUMN edc_policies.permissions IS 'Java List<Permission> serialized as JSON';
COMMENT ON COLUMN edc_policies.prohibitions IS 'Java List<Prohibition> serialized as JSON';
COMMENT ON COLUMN edc_policies.duties IS 'Java List<Duty> serialized as JSON';
COMMENT ON COLUMN edc_policies.extensible_properties IS 'Java Map<String, Object> serialized as JSON';
COMMENT ON COLUMN edc_policies.policy_type IS 'Java PolicyType serialized as JSON';

CREATE UNIQUE INDEX IF NOT EXISTS edc_policies_id_uindex
    ON edc_policies (policy_id);
