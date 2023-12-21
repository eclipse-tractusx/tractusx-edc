--
--  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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


CREATE TABLE edc_business_partner_group
(
    bpn    VARCHAR                 NOT NULL
        CONSTRAINT edc_business_partner_group_pk
            PRIMARY KEY,
    groups JSON DEFAULT '[]'::JSON NOT NULL
);
