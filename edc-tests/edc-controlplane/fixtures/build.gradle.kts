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
    // api modules that the test classes may need
    testFixturesApi(project(":spi:edr-spi"))
    testFixturesApi(project(":edc-extensions:edr:edr-api-v2"))
    testFixturesApi(project(":spi:core-spi"))
    testFixturesApi(project(":spi:tokenrefresh-spi"))
    testFixturesApi(project(":spi:bdrs-client-spi"))

    testFixturesApi(libs.edc.spi.core)
    testFixturesApi(libs.edc.spi.policy)
    testFixturesApi(libs.edc.spi.contract)
    testFixturesApi(testFixtures(libs.edc.api.management.test.fixtures))
    testFixturesApi(libs.edc.spi.edrstore)
    testFixturesApi(libs.edc.lib.cryptocommon)
    testFixturesApi(libs.edc.lib.boot)

    // api modules for some test utils
    testFixturesApi(libs.netty.mockserver)
    testFixturesApi(libs.edc.junit)
    testFixturesApi(libs.restAssured)
    testFixturesApi(libs.awaitility)

    testFixturesImplementation(libs.edc.identity.trust.sts.embedded)
    testFixturesImplementation(libs.edc.core.token)
    testFixturesImplementation(libs.edc.spi.identity.did)
    testFixturesImplementation(libs.postgres)
    testFixturesImplementation(libs.testcontainers.postgres)
    testFixturesImplementation(libs.assertj)
    testFixturesImplementation(libs.junit.jupiter.api)
    testFixturesImplementation(project(":edc-extensions:bpn-validation:bpn-validation-spi"))
    testFixturesImplementation(project(":edc-extensions:agreements:retirement-evaluation-spi"))

    testCompileOnly(project(":edc-tests:runtime:runtime-memory"))
    testCompileOnly(project(":edc-tests:runtime:runtime-postgresql"))
}

// do not publish
edcBuild {
    publish.set(false)
}
