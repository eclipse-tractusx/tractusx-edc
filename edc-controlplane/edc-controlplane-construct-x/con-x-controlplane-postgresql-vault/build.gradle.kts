/********************************************************************************
 * Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 * Copyright (c) 2026 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
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
 */

plugins {
    `java-library`
    id("application")
    alias(libs.plugins.shadow)
}

val edcVersion = project.property("con-x-edcVersion") as String
val txVersion = project.property("version") as String

dependencies {
    implementation("org.eclipse.edc:controlplane-dcp-bom:$edcVersion")
    implementation("org.eclipse.edc:controlplane-feature-sql-bom:$edcVersion")

    implementation(project(":edc-extensions:sql-vault"))

    implementation("org.eclipse.tractusx.edc:agreements:$txVersion")
    implementation("org.eclipse.tractusx.edc:retirement-evaluation-store-sql:$txVersion")
    implementation(project(":edc-extensions:agreements:retirement-evaluation-bootstrapping"))
    implementation("org.eclipse.tractusx.edc:tx-dcp:$txVersion")
    implementation(project(":edc-extensions:dynamic-issuers"))



    // Hotfix version overrides (19-10-2026):
    implementation("com.fasterxml.jackson.core:jackson-core:2.22.3")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.22.3")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    mergeServiceFiles()
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    archiveFileName.set("con-x-controlplane-postgresql-vault.jar")
    transform(com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer())
}

tasks.named("build") {
    dependsOn(tasks.named("shadowJar"))
}

application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}