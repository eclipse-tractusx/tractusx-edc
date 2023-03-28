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

import java.util.Locale;
import java.util.Objects;

import static org.eclipse.tractusx.edc.tests.Constants.AWS_ACCESS_KEY_ID;
import static org.eclipse.tractusx.edc.tests.Constants.AWS_SECRET_ACCESS_KEY;
import static org.eclipse.tractusx.edc.tests.Constants.BACKEND_SERVICE_BACKEND_API_URL;
import static org.eclipse.tractusx.edc.tests.Constants.DATABASE_PASSWORD;
import static org.eclipse.tractusx.edc.tests.Constants.DATABASE_URL;
import static org.eclipse.tractusx.edc.tests.Constants.DATABASE_USER;
import static org.eclipse.tractusx.edc.tests.Constants.DATA_MANAGEMENT_API_AUTH_KEY;
import static org.eclipse.tractusx.edc.tests.Constants.DATA_MANAGEMENT_URL;
import static org.eclipse.tractusx.edc.tests.Constants.DATA_PLANE_URL;
import static org.eclipse.tractusx.edc.tests.Constants.EDC_AWS_ENDPOINT_OVERRIDE;
import static org.eclipse.tractusx.edc.tests.Constants.IDS_URL;

public class Environment {

    private String awsEndpointOverride;
    private String awsAccessKey;
    private String awsSecretAccessKey;
    private String dataManagementAuthKey;
    private String dataManagementUrl;
    private String idsUrl;
    private String dataPlaneUrl;
    private String backendServiceBackendApiUrl;
    private String databaseUrl;
    private String databaseUser;
    private String databasePassword;

    private Environment() {

    }


    public static Environment byName(String name) {
        var upperName = name.toUpperCase(Locale.ROOT);

        return Environment.Builder.newInstance()
                .dataManagementUrl(System.getenv(String.join("_", upperName, DATA_MANAGEMENT_URL)))
                .dataManagementAuthKey(System.getenv(String.join("_", upperName, DATA_MANAGEMENT_API_AUTH_KEY)))
                .idsUrl(System.getenv(String.join("_", upperName, IDS_URL)))
                .dataPlaneUrl(System.getenv(String.join("_", upperName, DATA_PLANE_URL)))
                .backendServiceBackendApiUrl(
                        System.getenv(String.join("_", upperName, BACKEND_SERVICE_BACKEND_API_URL)))
                .databaseUrl(System.getenv(String.join("_", upperName, DATABASE_URL)))
                .databaseUser(System.getenv(String.join("_", upperName, DATABASE_USER)))
                .databasePassword(System.getenv(String.join("_", upperName, DATABASE_PASSWORD)))
                .awsEndpointOverride(System.getenv(EDC_AWS_ENDPOINT_OVERRIDE))
                .awsAccessKey(System.getenv(String.join("_", upperName, AWS_ACCESS_KEY_ID)))
                .awsSecretAccessKey(System.getenv(String.join("_", upperName, AWS_SECRET_ACCESS_KEY)))
                .build();
    }

    public String getIdsUrl() {
        return idsUrl;
    }

    public String getAwsEndpointOverride() {
        return awsEndpointOverride;
    }

    public String getAwsSecretAccessKey() {
        return awsSecretAccessKey;
    }

    public String getAwsAccessKey() {
        return awsAccessKey;
    }

    public String getBackendServiceBackendApiUrl() {
        return backendServiceBackendApiUrl;
    }

    public String getDatabasePassword() {
        return databasePassword;
    }

    public String getDatabaseUrl() {
        return databaseUrl;
    }

    public String getDatabaseUser() {
        return databaseUser;
    }

    public String getDataManagementAuthKey() {
        return dataManagementAuthKey;
    }

    public String getDataManagementUrl() {
        return dataManagementUrl;
    }

    private static class Builder {


        private final Environment environment;

        private Builder() {
            environment = new Environment();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder awsEndpointOverride(String val) {
            environment.awsEndpointOverride = val;
            return this;
        }

        public Builder awsAccessKey(String val) {
            environment.awsAccessKey = val;
            return this;
        }

        public Builder awsSecretAccessKey(String val) {
            environment.awsSecretAccessKey = val;
            return this;
        }

        public Builder dataManagementAuthKey(String val) {
            environment.dataManagementAuthKey = val;
            return this;
        }

        public Builder dataManagementUrl(String val) {
            environment.dataManagementUrl = val;
            return this;
        }

        public Builder idsUrl(String val) {
            environment.idsUrl = val;
            return this;
        }

        public Builder dataPlaneUrl(String val) {
            environment.dataPlaneUrl = val;
            return this;
        }

        public Builder backendServiceBackendApiUrl(String val) {
            environment.backendServiceBackendApiUrl = val;
            return this;
        }

        public Builder databaseUrl(String val) {
            environment.databaseUrl = val;
            return this;
        }

        public Builder databaseUser(String val) {
            environment.databaseUser = val;
            return this;
        }

        public Builder databasePassword(String val) {
            environment.databasePassword = val;
            return this;
        }

        public Environment build() {
            Objects.requireNonNull(environment.awsAccessKey);
            Objects.requireNonNull(environment.awsEndpointOverride);
            Objects.requireNonNull(environment.awsSecretAccessKey);
            Objects.requireNonNull(environment.backendServiceBackendApiUrl);
            Objects.requireNonNull(environment.databaseUrl);
            Objects.requireNonNull(environment.databasePassword);
            Objects.requireNonNull(environment.databaseUser);
            Objects.requireNonNull(environment.dataManagementUrl);
            Objects.requireNonNull(environment.dataPlaneUrl);
            Objects.requireNonNull(environment.dataManagementAuthKey);
            Objects.requireNonNull(environment.idsUrl);
            return environment;
        }
    }
}
