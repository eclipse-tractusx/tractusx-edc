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
    `java-test-fixtures`
}

dependencies {
    testFixturesApi(project(":spi:bdrs-client-spi"))
    testFixturesApi(project(":spi:core-spi"))
    testFixturesApi(project(":spi:edr-spi"))
    testFixturesApi(project(":edc-extensions:agreements:retirement-evaluation-spi"))
    testFixturesApi(project(":edc-extensions:bpn-validation:bpn-validation-spi"))


    testFixturesApi(libs.edc.core.token)
    testFixturesApi(libs.edc.junit)
    testFixturesApi(libs.edc.lib.cryptocommon)
    testFixturesApi(libs.edc.lib.jws2020)
    testFixturesApi(libs.edc.lib.token)
    testFixturesApi(libs.edc.lib.util)
    testFixturesApi(libs.edc.aws.s3.core)
    testFixturesApi(libs.edc.spi.edrstore)
    testFixturesApi(libs.edc.spi.jsonld)
    testFixturesApi(libs.edc.spi.identity.trust)
    testFixturesApi(libs.edc.spi.identity.did)
    testFixturesApi(libs.edc.spi.policy)
    testFixturesApi(libs.edc.spi.transfer)
    testFixturesApi(testFixtures(libs.edc.api.management.test.fixtures))

    testFixturesApi(libs.awaitility)
    testFixturesApi(libs.aws.s3)
    testFixturesApi(libs.azure.storage.blob)
    testFixturesApi(libs.jakartaJson)
    testFixturesApi(libs.netty.mockserver)
    testFixturesApi(libs.postgres)
    testFixturesApi(libs.restAssured)
    testFixturesApi(libs.testcontainers.junit)
    testFixturesApi(libs.testcontainers.minio)
    testFixturesApi(libs.testcontainers.postgres)
}

edcBuild {
    publish.set(false)
}
