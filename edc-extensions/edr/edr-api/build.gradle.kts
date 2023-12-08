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
    implementation(project(":spi:callback-spi"))
    implementation(project(":spi:edr-spi"))
    implementation(project(":spi:core-spi"))

    implementation(libs.edc.api.management)
    implementation(libs.edc.core.validator)
    implementation(libs.jakarta.rsApi)

    testImplementation(testFixtures(libs.edc.core.jersey))
    testImplementation(libs.restAssured)
    testImplementation(libs.edc.junit)
    testImplementation(libs.edc.ext.jersey.providers)
    testImplementation(libs.edc.core.transform)
}
