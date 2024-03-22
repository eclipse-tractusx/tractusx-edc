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
}

dependencies {
    testImplementation(project(":spi:edr-spi"))
    testImplementation(project(":edc-extensions:edr:edr-api-v2"))
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.restAssured)
    testImplementation(libs.nimbus.jwt)
    testImplementation(libs.postgres)
    testImplementation(libs.awaitility)
    testImplementation(libs.aws.s3)
    testImplementation(libs.edc.spi.core)
    testImplementation(libs.edc.junit)
    testImplementation(libs.edc.spi.policy)
    testImplementation(libs.edc.spi.contract)
    testImplementation(libs.edc.core.api)
    testImplementation(libs.edc.spi.catalog)
    testImplementation(libs.edc.api.catalog)
    testImplementation(libs.edc.api.contractnegotiation)
    testImplementation(libs.edc.api.transferprocess)
    testImplementation(libs.edc.spi.dataplane.selector)
    testImplementation(libs.edc.ext.jsonld)
    testImplementation(libs.edc.dsp)
    testImplementation(libs.edc.lib.http)
    testImplementation(testFixtures(libs.edc.sql.core))


    testCompileOnly(project(":edc-tests:runtime:extensions"))
    testCompileOnly(project(":edc-tests:runtime:runtime-memory"))
    testCompileOnly(project(":edc-tests:runtime:runtime-memory-ssi"))
    testCompileOnly(project(":edc-tests:runtime:runtime-postgresql"))
    testImplementation(project(":edc-extensions:ssi:ssi-miw-credential-client"))
    testImplementation(libs.edc.auth.oauth2.client)

    runtimeOnly(libs.tink)

}

// do not publish
edcBuild {
    publish.set(false)
}
