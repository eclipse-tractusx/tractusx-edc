/*
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
 */

plugins {
    `maven-publish`
    `java-library`
}

dependencies {
    implementation(libs.edc.runtime.metamodel)

    implementation(libs.edc.spi.boot)
    implementation(libs.edc.spi.core)
    implementation(libs.edc.spi.decentralized.claims)
    implementation(libs.edc.spi.participant.context.single)
    implementation(libs.edc.ih.spi.credentials)

    implementation(libs.dsp.spi.http)
    implementation(libs.dsp.spi.v2025)

    implementation(libs.edc.spi.participant)
    implementation(libs.edc.spi.protocol)

    implementation(project(":spi:core-spi"))
    implementation(project(":core:core-utils"))
    implementation(project(":edc-extensions:dataspace-protocol:dataspace-protocol-lib"))

    testImplementation(libs.edc.junit)
}
