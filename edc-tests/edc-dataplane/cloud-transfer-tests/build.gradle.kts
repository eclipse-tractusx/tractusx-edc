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

    testImplementation(project(":edc-tests:e2e-tests"))
    testImplementation(libs.edc.junit)
    testImplementation(libs.restAssured)

    // test runtime config
    testImplementation(libs.edc.config.filesystem)
    testImplementation(libs.edc.dpf.http)
    testImplementation(libs.edc.auth.tokenbased)
    testImplementation(libs.testcontainers.junit)
    testImplementation(testFixtures(libs.edc.azure.test))
    testImplementation(libs.edc.aws.s3.core)
    testImplementation(testFixtures(libs.edc.aws.s3.test))
    testImplementation(libs.aws.s3)
    testImplementation(libs.aws.s3transfer)
}



