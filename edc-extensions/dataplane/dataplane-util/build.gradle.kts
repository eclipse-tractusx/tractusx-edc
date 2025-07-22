/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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
}

dependencies {
    api(libs.edc.spi.dataplane.dataplane)

    implementation(libs.edc.lib.util)

    implementation(libs.opentelemetry.instrumentation.annotations)

    implementation(libs.edc.dpf.http)
    implementation(project(":edc-extensions:dataplane:dataplane-proxy:dataplane-proxy-http"))

    implementation(project(":spi:core-spi"))

    testImplementation(libs.edc.junit)

}


