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
    implementation(libs.edc.spi.core)
    implementation(libs.edc.config.filesystem)
    implementation(libs.edc.util)
    implementation(libs.edc.spi.aggregateservices)
    implementation(libs.edc.spi.contract)
    implementation(libs.edc.spi.controlplane)
    implementation(libs.edc.statemachine)

    implementation(project(":spi:edr-spi"))
    implementation(project(":spi:core-spi"))


    testImplementation(libs.edc.junit)
    testImplementation(libs.awaitility)
    testImplementation(testFixtures(project(":spi:edr-spi")))

}

