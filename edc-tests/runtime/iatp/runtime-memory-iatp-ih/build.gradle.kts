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

configurations.all {
    // the EDC credential verifier modules are excluded because this runtime embeds the IdentityHub, which pulls them in again;
    // the Tractus-X variants (:edc-extensions:jwt-verifiable-credentials, :edc-extensions:ldp-verifiable-credentials,
    // :edc-extensions:verifiable-credentials, :edc-extensions:json-ld-lib) must be the only copies on the classpath
    exclude(group = "org.eclipse.edc", module = "jwt-verifiable-credentials")
    exclude(group = "org.eclipse.edc", module = "ldp-verifiable-credentials")
    exclude(group = "org.eclipse.edc", module = "verifiable-credentials")
    exclude(group = "org.eclipse.edc", module = "json-ld-lib")
}

dependencies {

    // use basic (all in-mem) control plane
    implementation(project(":edc-controlplane:edc-controlplane-base"))
    implementation(project(":edc-extensions:jwt-verifiable-credentials"))
    implementation(project(":edc-extensions:ldp-verifiable-credentials"))
    implementation(project(":edc-extensions:verifiable-credentials"))
    implementation(project(":edc-extensions:json-ld-lib"))
    implementation(project(":edc-extensions:single-participant-vault"))
    implementation(project(":edc-extensions:cx-policy"))
    implementation(project(":edc-extensions:cx-policy-legacy"))
    implementation(project(":core:json-ld-core"))
    implementation(project(":core:json-ld-cx"))
    implementation(project(":edc-extensions:dcp:cx-dcp"))
    implementation(project(":edc-extensions:dcp:tx-dcp"))

    implementation(project(":edc-tests:runtime:iatp:iatp-extensions"))

    // use basic (all in-mem) data plane
    implementation(project(":edc-dataplane:edc-dataplane-base")) {
        exclude("org.eclipse.edc", "api-observability")
        exclude("org.eclipse.edc", "data-plane-selector-client")
    }

    constraints {
        implementation("com.networknt:json-schema-validator:3.0.0") {
            because("older versions cause runtime issues")
        }
    }

    implementation(libs.edc.core.controlplane)
    implementation(libs.edc.core.did)
    implementation(libs.edc.decentralized.claims.transform)
    implementation(libs.edc.auth.oauth2.client)
    // IH dependencies
    implementation(libs.edc.ih.api.presentation)
    implementation(libs.edc.ih.keypairs)
    implementation(libs.edc.ih.participants)
    implementation(libs.edc.sts.account.provisioner)
    implementation(libs.edc.ih.did)
    implementation(libs.edc.ih.common.core)
    implementation(libs.edc.ih.core)

    // for the controller
    implementation(libs.jakarta.rsApi)
}

application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}

edcBuild {
    publish.set(false)
}
