/*
 * Copyright (c) 2025 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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
    id(libs.plugins.swagger.get().pluginId)
}

dependencies {

    api(project(":spi:core-spi"))
    api(project(":spi:bdrs-client-spi"))

    api(libs.edc.spi.transform)
    api(libs.edc.spi.web)
    api(libs.edc.spi.http)
    api(libs.edc.spi.jsonld)
    api(libs.edc.spi.controlplane)
    api(libs.dsp.spi.v08)
    api(libs.dsp.spi.v2025)
    api(libs.edc.spi.identity.did)

    implementation(libs.edc.lib.jersey.providers)
    implementation(libs.edc.lib.validator)
    implementation(libs.edc.lib.util)
    implementation(libs.edc.boot)
    implementation(libs.edc.api.management.config)
    implementation(libs.jakarta.rsApi)

    testImplementation(libs.edc.junit)
    testImplementation(libs.restAssured)
    testImplementation(testFixtures(libs.edc.core.jersey))
}

edcBuild {
    swagger {
        apiGroup.set("control-plane")
    }
}
