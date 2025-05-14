import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

/********************************************************************************
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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
    alias(libs.plugins.shadow)
    id(libs.plugins.swagger.get().pluginId)
}


dependencies {
    // compile-time dependencies
    implementation(libs.edc.spi.boot)
    implementation(libs.edc.spi.controlplane)
    implementation(libs.edc.lib.util)

    // runtime dependencies
    runtimeOnly(libs.edc.core.runtime)
    runtimeOnly(libs.edc.core.connector)
    runtimeOnly(libs.edc.boot)
    runtimeOnly(libs.edc.api.management) {
        exclude("org.eclipse.edc", "edr-cache-api")
    }
    runtimeOnly(libs.edc.api.management.config)

    runtimeOnly(libs.edc.ext.http)
    runtimeOnly(libs.bundles.edc.monitoring)

    // edc libs
    runtimeOnly(libs.edc.ext.jsonld)

    testImplementation(libs.edc.junit)
}

application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}

edcBuild {
    publish.set(false)
}

tasks.withType<ShadowJar> {
    mergeServiceFiles()
    archiveFileName.set("${project.name}.jar")
}


application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}
