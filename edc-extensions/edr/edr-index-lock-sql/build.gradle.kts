/*
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
    `java-library`
}


dependencies {

    implementation(libs.edc.lib.util)
    implementation(libs.edc.sql.core)
    implementation(libs.edc.sql.edrindex)
    implementation(libs.edc.spi.transactionspi)
    implementation(libs.edc.spi.transaction.datasource)
    implementation(libs.edc.spi.edrstore)
    api(libs.edc.spi.edrstore)


    implementation(project(":spi:core-spi"))
    implementation(project(":spi:edr-spi"))
}
