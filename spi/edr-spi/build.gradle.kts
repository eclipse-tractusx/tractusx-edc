/********************************************************************************
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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
    `java-test-fixtures`
}

dependencies {
    implementation(project(":spi:core-spi"))
    implementation(libs.edc.spi.core)
    implementation(libs.edc.spi.contract)
    implementation(libs.edc.spi.edrstore)

    testFixturesImplementation(project(":spi:core-spi"))
    testFixturesImplementation(libs.edc.junit)
    testFixturesImplementation(libs.junit.jupiter.api)
    testFixturesImplementation(libs.assertj)
    testFixturesImplementation(libs.awaitility)
    testFixturesImplementation(libs.edc.spi.edrstore)

}

