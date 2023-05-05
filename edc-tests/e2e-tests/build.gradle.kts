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
    testImplementation("com.squareup.okhttp3:mockwebserver:5.0.0-alpha.11")
    testImplementation(libs.restAssured)
    testImplementation(libs.postgres)
    testImplementation(libs.awaitility)
    testImplementation(libs.aws.s3)
    testImplementation(libs.edc.spi.core)
    testImplementation(libs.edc.junit)
    testImplementation(libs.edc.spi.policy)
    testImplementation(libs.edc.spi.contract)
    testImplementation(libs.edc.core.api)
    testImplementation(libs.edc.spi.catalog)
    testImplementation(libs.edc.api.catalog)
    testImplementation(libs.edc.api.contractnegotiation)
    testImplementation(libs.edc.api.transferprocess)
    testImplementation(libs.edc.spi.dataplane.selector)
    testImplementation(project(":edc-extensions:control-plane-adapter-api"))
}

// do not publish
edcBuild {
    publish.set(false)
}
