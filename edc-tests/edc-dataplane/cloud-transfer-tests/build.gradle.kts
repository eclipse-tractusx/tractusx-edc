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

plugins {
    `java-library`
}

dependencies {

    testImplementation(project(":edc-tests:e2e-tests"))
    testImplementation(libs.edc.junit)
    testImplementation(libs.restAssured)

    // test runtime config
    testImplementation(libs.edc.config.filesystem)
    testImplementation(libs.edc.dpf.http)
    testImplementation(libs.edc.auth.tokenbased)
    testImplementation(libs.edc.dpf.selector.spi)
    testImplementation(libs.testcontainers.junit)
    testImplementation(testFixtures(libs.edc.azure.test))
    testImplementation(libs.edc.aws.s3.core)
    testImplementation(testFixtures(libs.edc.aws.s3.test))
    testImplementation(libs.aws.s3)
    testImplementation(libs.aws.s3transfer)
}



