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
import org.eclipse.tractusx.edc.lifecycle.PgHashicorpParticipantRuntime;
import org.eclipse.tractusx.edc.lifecycle.PgParticipantRuntime;
import org.junit.jupiter.api.extension.RegisterExtension;

@PostgresqlDbIntegrationTest
public class NegotiateEdrPostgresqlHashicorpVaultTest extends AbstractNegotiateEdrTest {

    @RegisterExtension
    protected static final PgParticipantRuntime PLATO_RUNTIME = new PgParticipantRuntime(
            ":edc-tests:runtime:runtime-postgresql",
            PLATO.getName(),
            PLATO.getBpn(),
            PLATO.getConfiguration()
    );
    private static final String VAULT_DIRECTORY = "testDir/";

    @RegisterExtension
    protected static final PgHashicorpParticipantRuntime SOKRATES_RUNTIME = new PgHashicorpParticipantRuntime(
            ":edc-tests:runtime:runtime-postgresql-hashicorp",
            SOKRATES.getName(),
            SOKRATES.getBpn(),
            VAULT_DIRECTORY,
            SOKRATES.getConfiguration()
    );

}
