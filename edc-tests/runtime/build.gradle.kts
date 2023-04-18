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

    // use basic (all in-mem) control plane
    implementation(project(":edc-controlplane:edc-controlplane-base")) {
        exclude("org.eclipse.edc", "oauth2-core")
        exclude("org.eclipse.edc", "oauth2-daps")
        exclude(module = "data-encryption")
        exclude(module = "control-plane-adapter")
    }

    // use basic (all in-mem) data plane
    runtimeOnly(project(":edc-dataplane:edc-dataplane-base")) {
        exclude("org.eclipse.edc", "api-observability")
    }

    implementation(edc.core.controlplane)
    // for the controller
    implementation(libs.jakarta.rsApi)
}

application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    mergeServiceFiles()
    archiveFileName.set("app.jar")
}

// do not publish
edcBuild {
    publish.set(false)
}
