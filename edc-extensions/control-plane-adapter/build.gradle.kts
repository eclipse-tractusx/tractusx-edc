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
    `java-library`
    `maven-publish`
    id("io.swagger.core.v3.swagger-gradle-plugin")
}

dependencies {
    implementation(libs.edc.spi.core)
    implementation(libs.edc.spi.policy)

    implementation(libs.edc.api.management)
    constraints {
        implementation("org.yaml:snakeyaml:2.0") {
            because("version 1.33 has vulnerabilities: https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2022-1471.")
        }
    }

    implementation(libs.edc.spi.catalog)
    implementation(libs.edc.spi.transactionspi)
    implementation(libs.edc.spi.transaction.datasource)
    implementation(libs.edc.dsp)
    implementation(libs.edc.util)
    implementation(libs.edc.sql.core)
    implementation(libs.edc.sql.lease)
    implementation(libs.edc.sql.pool)


    implementation(libs.postgres)
    implementation(libs.jakarta.rsApi)


    implementation(libs.edc.spi.aggregateservices)
    testImplementation(libs.awaitility)
}
