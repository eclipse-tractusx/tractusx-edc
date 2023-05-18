/*
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.tractusx.edc.lifecycle;


import org.junit.jupiter.api.extension.RegisterExtension;

import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.PLATO_BPN;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.PLATO_NAME;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.SOKRATES_BPN;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.SOKRATES_NAME;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.platoConfiguration;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.sokratesConfiguration;


public class MultiRuntimeTest {


    @RegisterExtension
    protected static ParticipantWrapper sokrates = new ParticipantWrapper(
            ":edc-tests:runtime:runtime-memory",
            SOKRATES_NAME,
            SOKRATES_BPN,
            sokratesConfiguration()
    );


    @RegisterExtension
    protected static ParticipantWrapper plato = new ParticipantWrapper(
            ":edc-tests:runtime:runtime-memory",
            PLATO_NAME,
            PLATO_BPN,
            platoConfiguration());
}
