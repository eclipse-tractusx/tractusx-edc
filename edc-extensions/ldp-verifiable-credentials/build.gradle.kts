/********************************************************************************
 * Copyright (c) 2026 Cofinity-X GmbH
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

// Tractus-X variant of EDC's `ldp-verifiable-credentials` module, using the same package and class names.
// The upstream module is excluded from the control-plane runtime so this one is the only copy on the classpath.
// Keep the class set and public API identical to the EDC version being replaced.
configurations.all {
    exclude(group = "org.eclipse.edc", module = "ldp-verifiable-credentials")
}

dependencies {
    implementation(libs.edc.spi.core)
    implementation(libs.edc.spi.jsonld)
    implementation(libs.edc.spi.identity.did)
    implementation(libs.edc.spi.decentralized.claims)
    implementation(libs.edc.lib.util)
    implementation(libs.titaniumJsonLd)
    implementation(libs.ironVerifiableCredentials)
    implementation(libs.jakartaJson)

    testImplementation(libs.edc.junit)
    testImplementation(libs.edc.lib.json)
    testImplementation(libs.edc.lib.jws2020)
    testImplementation(libs.edc.lib.jsonld)
    testImplementation(libs.nimbus.jwt)
}

tasks.test {
    useJUnitPlatform()
}
