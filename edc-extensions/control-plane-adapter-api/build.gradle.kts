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
    `maven-publish`
    id("io.swagger.core.v3.swagger-gradle-plugin")
}

dependencies {
    implementation(project(":spi:control-plane-adapter-spi"))
    implementation(project(":spi:edr-cache-spi"))
    implementation(project(":spi:core-spi"))

    implementation(libs.edc.api.management)
    implementation(libs.edc.spi.aggregateservices)
    implementation(libs.jakarta.rsApi)

    testImplementation(testFixtures(libs.edc.core.jersey))
    testImplementation(libs.restAssured)
    testImplementation(libs.edc.junit)
}
