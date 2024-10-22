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

import org.eclipse.tractusx.edc.tests.azure.AzuriteExtension;

public class TestConstants {
    // AZURE BLOB CONSTANTS
    public static final AzuriteExtension.Account PROVIDER_AZURITE_ACCOUNT = new AzuriteExtension.Account("provider", "providerkey");
    public static final AzuriteExtension.Account CONSUMER_AZURITE_ACCOUNT = new AzuriteExtension.Account("consumer", "consumerkey");
    // alias under which the provider key is stored in the vault. must end with -key1
    public static final String AZBLOB_PROVIDER_KEY_ALIAS = "provider-key1";
    // alias under which the consumer key is stored in the vault. must end with -key1
    public static final String AZBLOB_CONSUMER_KEY_ALIAS = "consumer-key1";
    // GENERIC CONSTANTS
    public static final String TESTFILE_NAME = "testfile.json";
    public static final String PREFIX_FOR_MUTIPLE_FILES = "m/";

}
