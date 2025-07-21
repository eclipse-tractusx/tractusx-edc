/********************************************************************************
 * Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
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
 ********************************************************************************/

plugins {
    `java-library`
    id(libs.plugins.swagger.get().pluginId)
}

configurations.all {
    // edr-cache-api excluded due to edr controller signature clash with tx-edr-api-v2 that provides same functionality with token auto_refresh capability
    exclude(group = "org.eclipse.edc", module = "edr-cache-api")

    // identity-trust-sts-remote-client excluded because we have the tx-dcp-sts-dim that takes care to define the correct client in case of DIM
    exclude("org.eclipse.edc", "identity-trust-sts-remote-client")
}

dependencies {
    runtimeOnly(libs.edc.bom.controlplane.base)
    runtimeOnly(libs.edc.bom.controlplane.dcp)

    runtimeOnly(libs.edc.bom.federatedcatalog.base)
    runtimeOnly(libs.edc.bom.federatedcatalog.dcp)

    implementation(project(":core:edr-core"))
    implementation(project(":core:json-ld-core"))
    implementation(project(":edc-extensions:log4j2-monitor"))
    implementation(project(":edc-extensions:agreements"))
    implementation(project(":edc-extensions:bdrs-client"))
    implementation(project(":edc-extensions:bpn-validation"))
    implementation(project(":edc-extensions:cx-policy"))
    implementation(project(":edc-extensions:data-flow-properties-provider"))
    implementation(project(":edc-extensions:dataplane:dataplane-selector-configuration"))
    implementation(project(":edc-extensions:dcp:tx-dcp"))
    implementation(project(":edc-extensions:dcp:tx-dcp-sts-dim"))
    implementation(project(":edc-extensions:edr:edr-api-v2"))
    implementation(project(":edc-extensions:edr:edr-callback"))
    implementation(project(":edc-extensions:federated-catalog"))
    implementation(project(":edc-extensions:provision-additional-headers"))
    implementation(project(":edc-extensions:tokenrefresh-handler"))
    implementation(project(":edc-extensions:validators:empty-asset-selector"))
    runtimeOnly(project(":edc-extensions:event-subscriber"))

    runtimeOnly(libs.bundles.edc.monitoring)
    runtimeOnly(libs.edc.aws.validator.data.address.s3)
    runtimeOnly(libs.edc.azure.blob.provision)
    runtimeOnly(libs.edc.aws.provision.s3)
    runtimeOnly(libs.edc.controlplane.callback.staticendpoint)
    runtimeOnly(libs.edc.validator.data.address.http.data)
    runtimeOnly(libs.log4j2.core)
    runtimeOnly(libs.log4j2.json.template)

}
