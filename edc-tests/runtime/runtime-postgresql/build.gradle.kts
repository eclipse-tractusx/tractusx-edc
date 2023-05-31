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
}


dependencies {

    // use basic (all in-mem) control plane
    implementation(project(":edc-controlplane:edc-controlplane-postgresql-hashicorp-vault")) {
        exclude("org.eclipse.edc", "oauth2-core")
        exclude("org.eclipse.edc", "oauth2-daps")
        exclude(module = "data-encryption")
        exclude(module = "hashicorp-vault")
    }

    implementation(project(":edc-tests:runtime:extensions"))

    // use basic (all in-mem) data plane
    runtimeOnly(project(":edc-dataplane:edc-dataplane-base")) {
        exclude("org.eclipse.edc", "api-observability")
    }


    implementation(libs.edc.core.controlplane)
    // for the controller
    implementation(libs.jakarta.rsApi)
}

application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}

// do not publish
edcBuild {
    publish.set(false)
}
