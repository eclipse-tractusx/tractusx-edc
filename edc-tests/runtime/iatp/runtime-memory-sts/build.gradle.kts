/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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
        exclude(module = "data-encryption")
        exclude(module = "ssi-identity-core")
        exclude(module = "ssi-miw-credential-client")
        exclude(module = "ssi-identity-extractor")
        exclude(module = "cx-policy")
    }
    implementation(project(":core:json-ld-core"))
    implementation(project(":edc-tests:runtime:extensions"))

    implementation(libs.edc.iam.mock)
    // for the controller
    implementation(libs.jakarta.rsApi)
    implementation(libs.bundles.edc.sts)

}

application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}

edcBuild {
    publish.set(false)
}
