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
    implementation(project(":edc-controlplane:edc-controlplane-base")) {
        exclude(module = "json-ld-core")
        exclude(module = "ssi-identity-core")
        exclude(module = "ssi-miw-credential-client")
        exclude(module = "ssi-identity-extractor")
        exclude(module = "cx-policy")
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

edcBuild {
    publish.set(false)
}
