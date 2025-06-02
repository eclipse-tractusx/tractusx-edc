/********************************************************************************
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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
    alias(libs.plugins.shadow)
}

dependencies {
    runtimeOnly(project(":edc-tests:runtime:iatp:runtime-memory-iatp-ih")) {
        exclude(module = "tx-dcp-sts-dim")

    }

    runtimeOnly(project(":edc-tests:runtime:iatp:runtime-memory-sts")) {
        exclude(module = "tx-dcp-sts-dim")
        exclude(group = "org.eclipse.edc", module = "iam-mock")
        exclude(module = "bdrs-client")

        exclude(module = "data-flow-properties-provider")
    }

    runtimeOnly(project(":samples:edc-dast:edc-dast-extensions"))
    runtimeOnly(project(":edc-extensions:bdrs-client"))
}

application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}

tasks.withType<ShadowJar> {
    mergeServiceFiles()
    archiveFileName.set("${project.name}.jar")
}

edcBuild {
    publish.set(false)
}
