/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial build file
 *
 */

plugins {
    `java-library`
    `java-test-fixtures`
    `maven-publish`
}


dependencies {
    api(libs.edc.spi.transaction.datasource)
    api(libs.edc.sql.core)
    implementation(libs.apache.commons.pool)

    testImplementation(libs.edc.junit)
    testImplementation(libs.edc.transaction.local)

}


