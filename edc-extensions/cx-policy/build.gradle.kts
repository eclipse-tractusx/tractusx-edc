/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

plugins {
    `java-library`
}

dependencies {
    implementation(project(":spi:core-spi"))
    implementation(project(":spi:ssi-spi"))
    implementation(libs.edc.spi.policyengine)
    implementation(libs.jakartaJson)
    testImplementation(libs.jacksonJsonP)
    testImplementation(libs.titaniumJsonLd)
    testImplementation(testFixtures(project(":spi:ssi-spi")))
}
