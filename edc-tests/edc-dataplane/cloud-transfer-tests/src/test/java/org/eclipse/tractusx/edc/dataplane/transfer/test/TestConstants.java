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

package org.eclipse.tractusx.edc.dataplane.transfer.test;

import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import software.amazon.awssdk.regions.Region;

import java.util.List;

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

public class TestConstants {
    // AZURE BLOB CONSTANTS
    public static final String AZURITE_DOCKER_IMAGE = "mcr.microsoft.com/azure-storage/azurite";
    public static final int AZURITE_CONTAINER_PORT = 10000;
    public static final AzuriteContainer.Account PROVIDER_AZURITE_ACCOUNT = new AzuriteContainer.Account("provider", "providerkey");
    public static final AzuriteContainer.Account CONSUMER_AZURITE_ACCOUNT = new AzuriteContainer.Account("consumer", "consumerkey");
    // alias under which the provider key is stored in the vault. must end with -key1
    public static final String AZBLOB_PROVIDER_KEY_ALIAS = "provider-key1";
    // alias under which the consumer key is stored in the vault. must end with -key1
    public static final String AZBLOB_CONSUMER_KEY_ALIAS = "consumer-key1";
    public static final String AZBLOB_PROVIDER_CONTAINER_NAME = "src-container";
    public static final String AZBLOB_CONSUMER_CONTAINER_NAME = "dest-container";
    // AMAZON S3 CONSTANTS
    public static final String S3_REGION = Region.US_WEST_2.id();
    public static final String S3_PROVIDER_BUCKET_NAME = "provider-bucket";
    public static final String S3_CONSUMER_BUCKET_NAME = "consumer-bucket";
    // GENERIC CONSTANTS
    public static final String TESTFILE_NAME = "testfile.json";
    public static final String PREFIX_FOR_MUTIPLE_FILES = "m/";

    public static JsonObjectBuilder blobSourceAddress(List<JsonObjectBuilder> additionalProperties) {
        return Json.createObjectBuilder()
                .add("dspace:endpointType", "AzureStorage")
                .add("dspace:endpointProperties", Json.createArrayBuilder(additionalProperties)
                        .add(dspaceProperty(EDC_NAMESPACE + "container", AZBLOB_PROVIDER_CONTAINER_NAME))
                        .add(dspaceProperty(EDC_NAMESPACE + "account", PROVIDER_AZURITE_ACCOUNT.name()))
                        .add(dspaceProperty(EDC_NAMESPACE + "keyName", PROVIDER_AZURITE_ACCOUNT.key()))
                );
    }

    public static JsonObjectBuilder blobDestinationAddress(List<JsonObjectBuilder> additionalProperties) {
        return Json.createObjectBuilder()
                .add("dspace:endpointType", "AzureStorage")
                .add("dspace:endpointProperties", Json.createArrayBuilder(additionalProperties)
                        .add(dspaceProperty(EDC_NAMESPACE + "container", AZBLOB_CONSUMER_CONTAINER_NAME))
                        .add(dspaceProperty(EDC_NAMESPACE + "account", CONSUMER_AZURITE_ACCOUNT.name()))
                        .add(dspaceProperty(EDC_NAMESPACE + "keyName", CONSUMER_AZURITE_ACCOUNT.key()))
                );
    }

    private static JsonObjectBuilder dspaceProperty(String name, String value) {
        return Json.createObjectBuilder()
                .add("dspace:name", name)
                .add("dspace:value", value);
    }

}
