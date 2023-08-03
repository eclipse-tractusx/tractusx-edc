/*
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
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
}

dependencies {
    implementation(project(":edc-extensions:bpn-validation:bpn-validation-spi"))

    implementation(libs.edc.spi.core)
    implementation(libs.edc.spi.transaction.datasource)
    implementation(libs.edc.spi.transactionspi)
    implementation(libs.edc.core.sql)


    testImplementation(libs.edc.transaction.local)
    testImplementation(testFixtures(libs.edc.core.sql))
    testImplementation(testFixtures(libs.edc.junit))
    testImplementation(testFixtures(project(":edc-extensions:bpn-validation:bpn-validation-core")))
}
