/*
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft
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
 */

plugins {
    `java-library`
}

dependencies {
    api(project(":spi:tokenrefresh-spi"))
    api(project(":spi:core-spi"))
    implementation(project(":core:core-utils"))
    implementation(libs.edc.spi.core)
    implementation(libs.edc.spi.dataplane.dataplane)
    implementation(libs.edc.spi.identity.did)
    implementation(libs.edc.spi.jwt)
    implementation(libs.edc.spi.jwt.signer)
    implementation(libs.edc.spi.keys)
    implementation(libs.edc.spi.token)
    implementation(libs.edc.core.token)
    implementation(libs.edc.lib.cryptocommon)
    implementation(libs.edc.lib.query)
    implementation(libs.edc.lib.token)

    testImplementation(libs.edc.junit)
    testImplementation(project(":edc-dataplane:edc-dataplane-core"))
    testImplementation(libs.edc.core.connector)
    testImplementation(libs.edc.lib.boot)
    testImplementation(libs.edc.lib.token)
}

