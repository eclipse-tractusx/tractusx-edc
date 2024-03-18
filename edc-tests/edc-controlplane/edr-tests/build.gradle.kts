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
    testImplementation(project(":spi:edr-spi"))
    testImplementation(project(":edc-extensions:edr:edr-api"))
    testImplementation(project(":spi:core-spi"))
    testImplementation(project(":spi:tokenrefresh-spi"))

    testImplementation(testFixtures(libs.edc.api.management.test.fixtures))
    testImplementation(libs.edc.spi.edrstore)
    testImplementation(libs.edc.identity.trust.sts.embedded)
    testImplementation(libs.netty.mockserver)
    testImplementation(libs.edc.core.token)
    testImplementation(libs.edc.junit)
    testImplementation(libs.restAssured)

    testCompileOnly(project(":edc-tests:runtime:runtime-memory"))

    testFixturesImplementation(libs.junit.jupiter.api)
    testFixturesImplementation(libs.edc.spi.core)
    testFixturesImplementation(libs.edc.junit)
    testFixturesImplementation(libs.edc.spi.policy)
    testFixturesImplementation(libs.edc.spi.contract)
}

// do not publish
edcBuild {
    publish.set(false)
}
