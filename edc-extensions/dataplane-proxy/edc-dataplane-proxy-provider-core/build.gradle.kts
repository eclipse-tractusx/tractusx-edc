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

    implementation(libs.edc.util)
    implementation(libs.edc.dpf.framework)
    implementation(libs.edc.dpf.util)
    implementation(libs.edc.jwt.core)
    implementation(libs.edc.ext.http)
    implementation(libs.edc.spi.http)

    implementation(libs.edc.spi.jwt)

    implementation(libs.jakarta.rsApi)
    implementation(libs.nimbus.jwt)

    implementation(project(":edc-extensions:dataplane-proxy:edc-dataplane-proxy-provider-spi"))
}

