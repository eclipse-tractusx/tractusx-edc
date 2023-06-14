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
}

dependencies {
    api(libs.edc.spi.jwt)
    implementation(libs.nimbus.jwt)
    implementation(libs.edc.spi.jsonld)
    implementation(libs.edc.jsonld)
    implementation(libs.edc.util)
    // used for the Ed25519 Verifier in conjunction with OctetKeyPairs (OKP)
    runtimeOnly(libs.tink)
    implementation(libs.jakartaJson)

    implementation(libs.apicatalog.iron.vc) {
        exclude("com.github.multiformats")
    }
}
