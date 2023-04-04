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

import static org.eclipse.edc.junit.testfixtures.TestUtils.getFreePort;

class TestRuntimeConfiguration {


    static final String IDS_PATH = "/api/v1/ids";
    static final int PLATO_CONNECTOR_PORT = getFreePort();
    static final int PLATO_MANAGEMENT_PORT = getFreePort();
    static final String PLATO_CONNECTOR_PATH = "/api";
    static final String PLATO_MANAGEMENT_PATH = "/api/v1/management";
    static final int PLATO_IDS_API_PORT = getFreePort();
    static final String PLATO_IDS_API = "http://localhost:" + PLATO_IDS_API_PORT;

    static final int SOKRATES_CONNECTOR_PORT = getFreePort();
    static final int SOKRATES_MANAGEMENT_PORT = getFreePort();
    static final String SOKRATES_CONNECTOR_PATH = "/api";
    static final String SOKRATES_MANAGEMENT_PATH = "/api/v1/management";
    static final int SOKRATES_IDS_API_PORT = getFreePort();
    static final String SOKRATES_IDS_API = "http://localhost:" + SOKRATES_IDS_API_PORT;

    static final String SOKRATES_PUBLIC_API_PORT = String.valueOf(getFreePort());
    static final String PLATO_PUBLIC_API_PORT = String.valueOf(getFreePort());

    static final String PLATO_DATAPLANE_CONTROL_PORT = String.valueOf(getFreePort());
    static final String SOKRATES_DATAPLANE_CONTROL_PORT = String.valueOf(getFreePort());

}
