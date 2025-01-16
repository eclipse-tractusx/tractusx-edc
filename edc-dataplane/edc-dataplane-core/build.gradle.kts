/********************************************************************************
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

plugins {
    `java-library`
}

dependencies {

    api(libs.edc.spi.core)
    api(libs.edc.spi.web)
    api(libs.edc.controlplane.apiclient)
    api(libs.edc.spi.dataplane.dataplane)
    api(libs.edc.state.machine)
    api(libs.edc.lib.query)

    runtimeOnly(libs.edc.spi.token)
    runtimeOnly(libs.edc.lib.util)
    runtimeOnly(libs.edc.dpf.util)

    implementation(libs.edc.lib.store)
    implementation(libs.opentelemetry.instrumentation.annotations)

    runtimeOnly(libs.edc.lib.query)
    testImplementation(libs.edc.junit)
    testImplementation(libs.awaitility)
    testImplementation(testFixtures(libs.edc.spi.dataplane.dataplane))
}


