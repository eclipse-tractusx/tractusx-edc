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
    testImplementation(libs.edc.junit)
    testImplementation(libs.restAssured)
    testImplementation(libs.okhttp.mockwebserver)

    // test runtime config
    testImplementation(libs.edc.config.filesystem)
    testImplementation(libs.edc.dpf.http)
    testImplementation(libs.edc.auth.tokenbased)
    testRuntimeOnly(libs.edc.dpf.core)
    testRuntimeOnly(libs.edc.controlplane.apiclient)
    testImplementation(project(":spi:edr-spi"))
    testImplementation(project(":core:edr-cache-core"))
    testImplementation(project(":edc-extensions:dataplane-proxy:edc-dataplane-proxy-consumer-api"))
    testImplementation(project(":edc-extensions:dataplane-proxy:edc-dataplane-proxy-provider-api"))
    testImplementation(project(":edc-extensions:dataplane-proxy:edc-dataplane-proxy-provider-core"))

}



