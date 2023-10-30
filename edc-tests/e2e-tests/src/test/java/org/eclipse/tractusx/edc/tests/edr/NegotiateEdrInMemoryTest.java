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


import com.nimbusds.jose.util.Base64;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.tractusx.edc.lifecycle.ParticipantRuntime;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.security.SecureRandom;

import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.PLATO_BPN;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.PLATO_NAME;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.SOKRATES_BPN;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.SOKRATES_NAME;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.platoConfiguration;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.sokratesConfiguration;

@EndToEndTest
public class NegotiateEdrInMemoryTest extends AbstractNegotiateEdrTest {

    @RegisterExtension
    protected static final ParticipantRuntime SOKRATES_RUNTIME = new ParticipantRuntime(
            ":edc-tests:runtime:runtime-memory",
            SOKRATES_NAME,
            SOKRATES_BPN,
            sokratesConfiguration()
    );

    @RegisterExtension
    protected static final ParticipantRuntime PLATO_RUNTIME = new ParticipantRuntime(
            ":edc-tests:runtime:runtime-memory",
            PLATO_NAME,
            PLATO_BPN,
            platoConfiguration()
    );

    @BeforeAll
    static void prepare() {
        var bytes = new byte[32];

        new SecureRandom().nextBytes(bytes);
        var value = Base64.encode(bytes).toString();
        var vault = SOKRATES_RUNTIME.getContext().getService(Vault.class);
        vault.storeSecret("test-alias", value);
        vault = PLATO_RUNTIME.getContext().getService(Vault.class);
        vault.storeSecret("test-alias", value);

    }
}
