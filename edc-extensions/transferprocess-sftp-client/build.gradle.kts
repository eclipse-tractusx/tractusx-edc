/*
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
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

plugins {
    `maven-publish`
    `java-library`
}

dependencies {
    implementation(project(":edc-extensions:transferprocess-sftp-common"))
    implementation(libs.edc.spi.core)
    implementation(libs.edc.spi.transfer)
    implementation(libs.edc.spi.policy)
    implementation(libs.edc.spi.dataplane.dataplane)
    implementation(libs.edc.dpf.util)
    implementation(libs.edc.dpf.core)
    implementation(libs.edc.policy.engine)
    implementation(libs.bouncyCastle.bcpkixJdk18on)

    implementation(libs.apache.sshd.core)
    implementation(libs.apache.sshd.sftp)

    testImplementation(libs.awaitility)
    testImplementation(libs.edc.junit)

    testImplementation(libs.mockito.inline)
    testImplementation(libs.testcontainers.junit)
}