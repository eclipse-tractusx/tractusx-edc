/*
 *  Copyright (c) 2025, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */
plugins {
    `java-library`
}

dependencies {
    api(libs.edc.spi.http)
    api(libs.edc.spi.dataplane.dataplane)
    api(libs.edc.spi.dataplane.http)
    implementation(libs.edc.lib.util)
    implementation(libs.edc.dpf.http)
    implementation(project(":spi:core-spi"))

    testImplementation(libs.edc.junit)
    testImplementation(libs.edc.core.runtime)
    testImplementation(libs.edc.dpf.core)
    testImplementation(libs.edc.ext.jsonld)
    testImplementation(libs.restAssured)
    testImplementation(libs.netty.mockserver)

    testImplementation(testFixtures(libs.edc.lib.http))
    testImplementation(testFixtures(libs.edc.spi.dataplane.dataplane))
}
