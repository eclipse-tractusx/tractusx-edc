/*
 * Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.tractusx.edc.tests;


import org.eclipse.tractusx.edc.tests.util.DatabaseCleaner;
import org.eclipse.tractusx.edc.tests.util.S3Client;

import static org.mockito.Mockito.mock;

public class Connector {

    private final String name;

    private final Environment environment;

    private final DataManagementAPI dataManagementAPI;

    private final DatabaseCleaner databaseCleaner;


    private final S3Client s3Client;

    public Connector(String name, Environment environment) {
        this.name = name;
        this.environment = environment;
        dataManagementAPI = loadDataManagementAPI();
        databaseCleaner = loadDatabaseCleaner();
        s3Client = createS3Client();
    }

    public BackendDataService getBackendServiceBackendAPI() {
        return mock(BackendDataService.class);
    }

    public DatabaseCleaner getDatabaseCleaner() {
        return databaseCleaner;
    }

    public DataManagementAPI getDataManagementAPI() {
        return dataManagementAPI;
    }

    public Environment getEnvironment() {
        return environment;
    }

    public S3Client getS3Client() {
        return s3Client;
    }

    public String getName() {
        return name;
    }

    private DataManagementAPI loadDataManagementAPI() {
        return new DataManagementAPI(
                environment.getDataManagementUrl(), environment.getDataManagementAuthKey());
    }

    private DatabaseCleaner loadDatabaseCleaner() {
        return new DatabaseCleaner(
                environment.getDatabaseUrl(),
                environment.getDatabaseUser(),
                environment.getDatabasePassword());
    }


    private S3Client createS3Client() {
        return new S3Client(environment);
    }
}
