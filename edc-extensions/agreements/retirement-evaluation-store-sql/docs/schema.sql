--
--  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
--
--  This program and the accompanying materials are made available under the
--  terms of the Apache License, Version 2.0 which is available at
--  https://www.apache.org/licenses/LICENSE-2.0
--
--  SPDX-License-Identifier: Apache-2.0
--
--  Contributors:
--        Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
--

--
-- table: edc_agreement_retirement
--
CREATE TABLE IF NOT EXISTS edc_agreement_retirement
(
    contract_agreement_id     VARCHAR      NOT NULL
        CONSTRAINT agreement_retirement_contract_agreement_id_pk PRIMARY KEY,
    reason                    VARCHAR(255) NOT NULL,
    agreement_retirement_date BIGINT       NOT NULL
);
