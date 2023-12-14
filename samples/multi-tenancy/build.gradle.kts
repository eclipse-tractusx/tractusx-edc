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
    id("application")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}


dependencies {
    implementation(libs.edc.boot)
    implementation(libs.edc.iam.mock)
    implementation(project(":edc-controlplane:edc-controlplane-base")) {
        exclude("org.eclipse.tractusx.edc", "data-encryption")
        exclude(module = "ssi-miw-credential-client")
        exclude(module = "ssi-identity-core")
        exclude(module = "auth-tokenbased")
    }
    implementation(libs.edc.core.controlplane)
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
