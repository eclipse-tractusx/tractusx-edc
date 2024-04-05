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
    id("com.github.johnrengelman.shadow") version "8.1.1"
}


dependencies {
    implementation(libs.edc.boot)
    implementation(libs.edc.iam.mock)
    implementation(project(":edc-controlplane:edc-controlplane-base")) {
        exclude(module = "ssi-miw-credential-client")
        exclude(module = "ssi-identity-core")
        exclude(module = "auth-tokenbased")
        // the token refresh extension is not needed
        exclude(module = "tx-iatp-sts-dim")
        exclude(module = "tokenrefresh-handler")
        exclude(module = "edr-core")
        exclude(module = "edr-api-v2")
        exclude(module = "edr-callback")
        exclude("org.eclipse.edc", "identity-trust-issuers-configuration")
    }
    implementation(libs.edc.core.controlplane)
    implementation(libs.jakarta.rsApi)
}

application {
    mainClass.set("org.eclipse.tractusx.edc.samples.multitenancy.MultiTenantRuntime")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    mergeServiceFiles()
    archiveFileName.set("multitenant.jar")
}

// do not publish
edcBuild {
    publish.set(false)
}
