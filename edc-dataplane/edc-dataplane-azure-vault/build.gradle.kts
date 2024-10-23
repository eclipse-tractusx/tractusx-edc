/********************************************************************************
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

plugins {
    `java-library`
    id("application")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

dependencies {
    implementation(project(":edc-dataplane:edc-dataplane-base"))
    runtimeOnly(project(":edc-extensions:migrations::data-plane-migration"))
    implementation(libs.edc.azure.vault)
    constraints {
        implementation("net.minidev:json-smart:2.5.1") {
            because("version 2.4.8 has vulnerabilities: https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2023-1370.")
        }
    }
    implementation(libs.edc.azure.identity)
    implementation("com.azure:azure-security-keyvault-secrets:4.9.0")
    runtimeOnly(libs.edc.transaction.local)
    runtimeOnly(libs.edc.sql.pool)
    runtimeOnly(libs.edc.sql.accesstokendata)
    runtimeOnly(libs.edc.sql.edrindex)
    runtimeOnly(libs.edc.sql.dataplane)
    runtimeOnly(libs.postgres)
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    mergeServiceFiles()
    archiveFileName.set("${project.name}.jar")
}

application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}
