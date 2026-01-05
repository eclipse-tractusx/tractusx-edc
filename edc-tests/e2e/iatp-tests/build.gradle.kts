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

configurations.all {
    exclude("com.networknt", "json-schema-validator")
}

dependencies {
    constraints {
        testImplementation("com.networknt:json-schema-validator:3.0.0") {
            because("older versions cause runtime issues")
        }
    }

    testImplementation(testFixtures(project(":edc-tests:e2e-fixtures")))
    testImplementation(libs.edc.spi.keypair)
    testImplementation(libs.edc.ih.spi)
    testImplementation(libs.edc.ih.spi.participant.context)
    testImplementation(libs.edc.ih.spi.credentials)
    testImplementation(libs.edc.junit)
    testImplementation(libs.edc.core.token)
    testImplementation(libs.edc.identity.vc.ldp)
    testImplementation(libs.edc.lib.jws2020)
    testImplementation(libs.edc.sts.core)
    testRuntimeOnly(libs.edc.transaction.local)

    testImplementation(libs.wiremock) {
        exclude("com.networknt", "json-schema-validator")
    }
    testImplementation(libs.restAssured)
    testImplementation(libs.awaitility)
    testImplementation(libs.bouncyCastle.bcpkixJdk18on)

    testCompileOnly(project(":edc-tests:runtime:iatp:runtime-memory-iatp-dim-ih"))
    testCompileOnly(project(":edc-tests:runtime:iatp:runtime-memory-iatp-ih"))
    testCompileOnly(project(":edc-tests:runtime:iatp:runtime-memory-sts"))
}

// do not publish
edcBuild {
    publish.set(false)
}
