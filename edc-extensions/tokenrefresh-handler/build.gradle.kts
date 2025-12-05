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
    `maven-publish`
    `java-library`
}

dependencies {
    implementation(project(":core:core-utils"))
    implementation(project(":spi:core-spi"))
    implementation(project(":spi:tokenrefresh-spi"))
    implementation(libs.edc.spi.core)
    implementation(libs.edc.spi.decentralized.claims)
    implementation(libs.edc.spi.edrstore)
    implementation(libs.edc.spi.http)
    implementation(libs.edc.spi.jwt)
    implementation(libs.edc.spi.participant.context.single)
    implementation(libs.edc.spi.token)
    implementation(libs.edc.lib.util)
    implementation(libs.nimbus.jwt)

    testImplementation(libs.edc.junit)
    testImplementation(libs.restAssured)
}
