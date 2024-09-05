/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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
    implementation(libs.edc.spi.dataplane.dataplane)
    implementation(libs.edc.spi.web)
    implementation(libs.edc.spi.dataplane.selector)
    implementation(libs.edc.spi.core)
    implementation(libs.edc.spi.transfer)
    implementation(libs.edc.spi.dataplane.transfer)

    testImplementation(libs.edc.junit)
}
