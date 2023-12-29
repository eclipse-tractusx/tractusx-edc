/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.tractusx.edc.tests.edr;

import org.eclipse.edc.junit.annotations.PostgresqlDbIntegrationTest;
import org.eclipse.tractusx.edc.lifecycle.PgParticipantRuntime;
import org.junit.jupiter.api.extension.RegisterExtension;

@PostgresqlDbIntegrationTest
public class DeleteEdrPostgresqlTest extends AbstractDeleteEdrTest {

    @RegisterExtension
    protected static final PgParticipantRuntime SOKRATES_RUNTIME = new PgParticipantRuntime(
            ":edc-tests:runtime:runtime-postgresql",
            SOKRATES.getName(),
            SOKRATES.getBpn(),
            SOKRATES.renewalConfiguration("5")
    );
    @RegisterExtension
    protected static final PgParticipantRuntime PLATO_RUNTIME = new PgParticipantRuntime(
            ":edc-tests:runtime:runtime-postgresql",
            PLATO.getName(),
            PLATO.getBpn(),
            PLATO.renewalConfiguration()
    );

}
