/********************************************************************************
 * Copyright (c) 2025 Cofinity-X GmbH
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
    java
}

dependencies {
    testImplementation(testFixtures(project(":edc-tests:e2e-fixtures")))
    testImplementation(libs.edc.junit)
    testImplementation(libs.edc.core.controlplane)
    testImplementation(libs.awaitility)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.dsp.tck.core)
    testImplementation(libs.dsp.tck.runtime)
    testImplementation(libs.dsp.tck.api)
    testImplementation(libs.dsp.tck.system)
    testRuntimeOnly(libs.dsp.tck.metadata)
    testRuntimeOnly(libs.dsp.tck.catalog)
    testRuntimeOnly(libs.dsp.tck.transferprocess)
    testRuntimeOnly(libs.dsp.tck.contractnegotiation)
    testImplementation(libs.junit.platform.launcher)
    testImplementation(libs.edc.spi.identitytrust)
    testImplementation(libs.nimbus.jwt)
}

edcBuild {
    publish.set(false)
}
