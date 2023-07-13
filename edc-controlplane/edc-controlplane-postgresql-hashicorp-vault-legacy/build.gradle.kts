/*
 * Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    `java-library`
    id("application")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

dependencies {
    runtimeOnly(project(":core:edr-cache-core"))
    runtimeOnly(project(":core:edr-core"))
    runtimeOnly(project(":edc-extensions:business-partner-validation"))
    runtimeOnly(project(":edc-extensions:dataplane-selector-configuration"))
    runtimeOnly(project(":edc-extensions:data-encryption"))
    runtimeOnly(project(":edc-extensions:cx-oauth2"))
    runtimeOnly(project(":edc-extensions:provision-additional-headers"))
    runtimeOnly(project(":edc-extensions:edr:edr-api"))
    runtimeOnly(project(":edc-extensions:edr:edr-callback"))

    runtimeOnly(libs.edc.core.controlplane)
    runtimeOnly(libs.edc.config.filesystem)
    runtimeOnly(libs.edc.auth.tokenbased)
    runtimeOnly(libs.edc.auth.oauth2.core)
    runtimeOnly(libs.edc.auth.oauth2.daps)
    runtimeOnly(libs.edc.api.management)
    runtimeOnly(libs.edc.api.observability)
    runtimeOnly(libs.edc.dsp)
    runtimeOnly(libs.edc.spi.jwt)
    runtimeOnly(libs.bundles.edc.dpf)

    runtimeOnly(libs.edc.ext.http)
    runtimeOnly(libs.bundles.edc.monitoring)
    runtimeOnly(libs.edc.transfer.dynamicreceiver)
    runtimeOnly(libs.edc.controlplane.callback.dispatcher.event)
    runtimeOnly(libs.edc.controlplane.callback.dispatcher.http)

    runtimeOnly(project(":edc-extensions:postgresql-migration"))
    runtimeOnly(project(":edc-extensions:hashicorp-vault"))
    runtimeOnly(project(":edc-extensions:edr:edr-cache-sql"))
    runtimeOnly(libs.bundles.edc.sqlstores)
    runtimeOnly(libs.edc.transaction.local)
    runtimeOnly(libs.edc.sql.pool)
    runtimeOnly(libs.edc.core.controlplane)
    runtimeOnly(libs.edc.dpf.transfer)
    runtimeOnly(libs.postgres)

    // needed for DAPS - not officially supported anymore
    runtimeOnly(project(":edc-extensions:cx-oauth2"))
    runtimeOnly(libs.edc.auth.oauth2.core)
    runtimeOnly(libs.edc.auth.oauth2.daps)
}


tasks.withType<ShadowJar> {
    exclude("**/pom.properties", "**/pom.xm")
    mergeServiceFiles()
    archiveFileName.set("${project.name}.jar")
}


application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}
