/********************************************************************************
 * Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
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
 ********************************************************************************/

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    `java-library`
    id("application")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

dependencies {
    runtimeOnly(project(":edc-controlplane:edc-controlplane-base"))
    runtimeOnly(project(":edc-extensions:migrations::control-plane-migration"))
    runtimeOnly(project(":edc-extensions:bpn-validation:business-partner-store-sql"))
    runtimeOnly(libs.edc.azure.vault)
    runtimeOnly(libs.bundles.edc.sqlstores)
    runtimeOnly(libs.edc.transaction.local)
    runtimeOnly(libs.edc.sql.pool)
    runtimeOnly(libs.edc.core.controlplane)
    runtimeOnly(libs.postgres)
}


tasks.withType<ShadowJar> {
    mergeServiceFiles()
    archiveFileName.set("${project.name}.jar")
}


application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}
