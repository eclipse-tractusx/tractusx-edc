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
}

dependencies {
    runtimeOnly(libs.edc.bom.controlplane.base)
    runtimeOnly(libs.edc.bom.controlplane.dcp)

    runtimeOnly(libs.edc.bom.federatedcatalog.base)
    runtimeOnly(libs.edc.bom.federatedcatalog.dcp)

    runtimeOnly(project(":core:edr-core"))
    runtimeOnly(project(":core:json-ld-core"))
    runtimeOnly(project(":edc-extensions:agreements"))
    runtimeOnly(project(":edc-extensions:bdrs-client"))
    runtimeOnly(project(":edc-extensions:bpn-validation"))
    runtimeOnly(project(":edc-extensions:cx-policy"))
    runtimeOnly(project(":edc-extensions:data-flow-properties-provider"))
    runtimeOnly(project(":edc-extensions:dataplane:dataplane-selector-configuration"))
    runtimeOnly(project(":edc-extensions:dcp:tx-dcp"))
    runtimeOnly(project(":edc-extensions:dcp:tx-dcp-sts-dim"))
    runtimeOnly(project(":edc-extensions:edr:edr-api-v2"))
    runtimeOnly(project(":edc-extensions:edr:edr-callback"))
    runtimeOnly(project(":edc-extensions:federated-catalog"))
    runtimeOnly(project(":edc-extensions:provision-additional-headers"))
    runtimeOnly(project(":edc-extensions:tokenrefresh-handler"))
    runtimeOnly(project(":edc-extensions:validators:empty-asset-selector"))

    runtimeOnly(libs.bundles.edc.monitoring)
    runtimeOnly(libs.edc.aws.validator.data.address.s3)
    runtimeOnly(libs.edc.azure.blob.provision)
    runtimeOnly(libs.edc.controlplane.callback.staticendpoint)
    runtimeOnly(libs.edc.validator.data.address.http.data)

}
