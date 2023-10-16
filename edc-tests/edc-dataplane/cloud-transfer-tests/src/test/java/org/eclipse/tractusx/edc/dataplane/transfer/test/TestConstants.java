/*
 *
 *   Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft
 *
 *   See the NOTICE file(s) distributed with this work for additional
 *   information regarding copyright ownership.
 *
 *   This program and the accompanying materials are made available under the
 *   terms of the Apache License, Version 2.0 which is available at
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *   WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *   License for the specific language governing permissions and limitations
 *   under the License.
 *
 *   SPDX-License-Identifier: Apache-2.0
 *
 */

package org.eclipse.tractusx.edc.dataplane.transfer.test;

import org.eclipse.edc.spi.types.domain.DataAddress;
import software.amazon.awssdk.regions.Region;

public class TestConstants {
    // AZURE BLOB CONSTANTS
    public static final String AZURITE_DOCKER_IMAGE = "mcr.microsoft.com/azure-storage/azurite";
    public static final int AZURITE_CONTAINER_PORT = 10000;
    public static final String AZBLOB_PROVIDER_ACCOUNT_NAME = "provider";
    public static final String AZBLOB_PROVIDER_ACCOUNT_KEY = "providerkey";
    public static final String AZBLOB_CONSUMER_ACCOUNT_NAME = "consumer";
    public static final String AZBLOB_CONSUMER_ACCOUNT_KEY = "consumerkey";
    // alias under which the provider key is stored in the vault. must end with -key1
    public static final String AZBLOB_PROVIDER_KEY_ALIAS = "providerkey-key1";
    // alias under which the consumer key is stored in the vault. must end with -key1
    public static final String AZBLOB_CONSUMER_KEY_ALIAS = "consumerkey-key1`";
    public static final String AZBLOB_PROVIDER_CONTAINER_NAME = "src-container";
    public static final String AZBLOB_CONSUMER_CONTAINER_NAME = "dest-container";
    // AMAZON S3 CONSTANTS
    public static final String MINIO_DOCKER_IMAGE = "bitnami/minio";
    public static final int MINIO_CONTAINER_PORT = 9000;
    public static final String S3_REGION = Region.US_WEST_2.id();
    public static final String S3_PROVIDER_BUCKET_NAME = "provider-bucket";
    public static final String S3_CONSUMER_BUCKET_NAME = "consumer-bucket";
    public static final String S3_ACCESS_KEY_ID = "test-access-key"; // user name
    // GENERIC CONSTANTS
    public static final String TESTFILE_NAME = "testfile.json";
    public static final String COMPLETION_MARKER = ".complete";

    public static DataAddress blobSourceAddress(String blobName) {
        return DataAddress.Builder.newInstance()
                .type("AzureStorage")
                .property("container", AZBLOB_PROVIDER_CONTAINER_NAME)
                .property("account", AZBLOB_PROVIDER_ACCOUNT_NAME)
                .property("keyName", AZBLOB_PROVIDER_KEY_ALIAS)
                .property("blobname", blobName)
                .build();
    }

    public static DataAddress blobDestinationAddress(String blobName) {
        return DataAddress.Builder.newInstance()
                .type("AzureStorage")
                .property("container", AZBLOB_CONSUMER_CONTAINER_NAME)
                .property("account", AZBLOB_CONSUMER_ACCOUNT_NAME)
                .property("blobname", blobName)
                .property("keyName", AZBLOB_CONSUMER_KEY_ALIAS)
                .build();
    }


}
