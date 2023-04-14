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
    testImplementation(edc.spi.core)
    testImplementation(edc.junit)
    testImplementation(edc.spi.policy)
    testImplementation(edc.spi.contract)
    testImplementation(edc.core.api)
    testImplementation(edc.spi.catalog)
    testImplementation(edc.api.catalog)
    testImplementation(edc.api.contractnegotiation)
    testImplementation(edc.api.transferprocess)
    testImplementation(edc.spi.dataplane.selector)

}

// do not publish
edcBuild {
    publish.set(false)
}
