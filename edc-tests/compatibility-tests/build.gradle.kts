/*******************************************************************************
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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
 ******************************************************************************/

plugins {
    `java-library`
}

configurations.all {
    exclude("org.eclipse.edc", "decentralized-claims-core")
}

dependencies {
    testImplementation(libs.edc.junit)
    testImplementation(libs.edc.lib.cryptocommon)
    testImplementation(libs.edc.lib.jws2020)
    testImplementation(libs.edc.transaction.local)
    testImplementation(libs.edc.spi.sts)
    testImplementation(libs.edc.iam.mock)
    testImplementation(libs.edc.ih.spi.did)
    testImplementation(libs.edc.ih.spi.credentials)
    testImplementation(libs.edc.ih.spi.participant.context)
    testImplementation(project(":spi:bdrs-client-spi"))
    testImplementation(libs.jakartaJson)
    testImplementation(libs.jacksonJsonP)
    testImplementation(libs.restAssured)
    testImplementation(libs.awaitility)
    testImplementation(libs.wiremock)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.postgres)
    testImplementation(testFixtures(libs.edc.api.management.test.fixtures))
    testImplementation(testFixtures(libs.edc.sql.test.fixtures))
    testImplementation(testFixtures(project(":edc-tests:e2e-fixtures")))
    testImplementation("com.networknt:json-schema-validator:2.0.0")
}
