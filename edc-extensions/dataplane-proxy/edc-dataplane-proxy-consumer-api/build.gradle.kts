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
    id("io.swagger.core.v3.swagger-gradle-plugin")
}

dependencies {

    implementation(libs.jakarta.rsApi)

    implementation(libs.edc.spi.http)
    implementation(libs.edc.spi.dataplane.http)
    implementation(libs.edc.util)
    implementation(libs.edc.dpf.util)
    implementation(libs.edc.ext.http)
    implementation(libs.edc.spi.auth)

    implementation(project(":spi:edr-spi"))

    testImplementation(libs.edc.junit)
    testImplementation(testFixtures(libs.edc.core.jersey))
    testImplementation(libs.restAssured)
}

