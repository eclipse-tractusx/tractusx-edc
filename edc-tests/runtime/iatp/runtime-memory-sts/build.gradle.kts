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

plugins {
    `java-library`
    id("application")
}

dependencies {

    // use basic (all in-mem) control plane
    implementation(project(":edc-controlplane:edc-controlplane-base")) {
        exclude(module = "ssi-identity-core")
        exclude(module = "ssi-miw-credential-client")
        exclude(module = "ssi-identity-extractor")
        exclude(module = "tx-iatp-sts-dim")
        exclude("org.eclipse.edc", "identity-trust-issuers-configuration")
    }
    implementation(project(":core:json-ld-core"))
    implementation(project(":edc-tests:runtime:extensions"))

    implementation(libs.edc.iam.mock)
    implementation(libs.edc.spi.keys)
    // for the controller
    implementation(libs.jakarta.rsApi)
    implementation(libs.bundles.edc.sts)

    implementation(libs.edc.identity.trust.sts.embedded)
    implementation(libs.edc.core.token)
}

application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}

edcBuild {
    publish.set(false)
}
