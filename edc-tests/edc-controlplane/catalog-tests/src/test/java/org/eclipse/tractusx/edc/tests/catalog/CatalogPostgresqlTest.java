/********************************************************************************
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/

package org.eclipse.tractusx.edc.tests.catalog;

import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.tractusx.edc.tests.runtimes.PgParticipantRuntime;
import org.junit.jupiter.api.extension.RegisterExtension;

@PostgresqlIntegrationTest
public class CatalogPostgresqlTest extends AbstractCatalogTest {

    @RegisterExtension
    protected static final PgParticipantRuntime SOKRATES_RUNTIME = new PgParticipantRuntime(
            ":edc-tests:runtime:runtime-postgresql",
            SOKRATES.getName(),
            SOKRATES.getBpn(),
            SOKRATES.getConfiguration()
    );
    @RegisterExtension
    protected static final PgParticipantRuntime PLATO_RUNTIME = new PgParticipantRuntime(
            ":edc-tests:runtime:runtime-postgresql",
            PLATO.getName(),
            PLATO.getBpn(),
            PLATO.getConfiguration()
    );
}
