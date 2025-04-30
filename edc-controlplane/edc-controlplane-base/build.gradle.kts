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

dependencies {
    runtimeOnly(project(":core:edr-core"))
    runtimeOnly(project(":edc-extensions:dataplane:dataplane-selector-configuration"))
    runtimeOnly(project(":edc-extensions:provision-additional-headers"))
    runtimeOnly(project(":edc-extensions:edr:edr-api-v2"))
    runtimeOnly(project(":edc-extensions:edr:edr-callback"))
    runtimeOnly(project(":edc-extensions:tokenrefresh-handler"))
    runtimeOnly(project(":edc-extensions:agreements"))
    runtimeOnly(project(":edc-extensions:validators:empty-asset-selector"))

    runtimeOnly(libs.edc.core.edrstore)
    runtimeOnly(libs.edc.edr.store.receiver)
    runtimeOnly(libs.edc.dpf.transfer.signaling)
    runtimeOnly(libs.edc.controlplane.callback.staticendpoint)

    // needed for BPN validation
    runtimeOnly(project(":edc-extensions:bpn-validation"))
    // Credentials CX policies
    runtimeOnly(project(":edc-extensions:cx-policy"))

    // needed for DCP integration
    runtimeOnly(project(":core:json-ld-core"))
    runtimeOnly(libs.edc.core.did)
    runtimeOnly(libs.edc.identity.did.web)
    runtimeOnly(libs.edc.core.identitytrust)
    runtimeOnly(libs.edc.identity.trust.transform)
    runtimeOnly(libs.edc.identity.trust.issuers.configuration)
    runtimeOnly(project(":edc-extensions:dcp:tx-dcp"))
    runtimeOnly(project(":edc-extensions:dcp:tx-dcp-sts-dim"))
    runtimeOnly(project(":edc-extensions:bdrs-client"))
    runtimeOnly(project(":edc-extensions:data-flow-properties-provider"))

    runtimeOnly(libs.edc.core.connector)
    runtimeOnly(libs.edc.core.controlplane)
    runtimeOnly(libs.edc.core.runtime)
    runtimeOnly(libs.edc.core.token)
    runtimeOnly(libs.edc.core.policy.monitor)
    runtimeOnly(libs.edc.config.filesystem)
    runtimeOnly(libs.edc.auth.oauth2.client)
    runtimeOnly(libs.edc.auth.tokenbased)
    runtimeOnly(libs.edc.auth.delegated)
    runtimeOnly(libs.edc.auth.configuration)
    runtimeOnly(libs.edc.validator.data.address.http.data)
    runtimeOnly(libs.edc.aws.validator.data.address.s3)
    runtimeOnly(libs.edc.data.plane.selector.control.api)

    runtimeOnly(libs.edc.api.management) {
        exclude("org.eclipse.edc", "edr-cache-api")
    }
    runtimeOnly(libs.edc.api.controlplane)
    runtimeOnly(libs.edc.api.management.config)
    runtimeOnly(libs.edc.api.control.config)
    runtimeOnly(libs.edc.api.core)
    runtimeOnly(libs.edc.api.observability)
    runtimeOnly(libs.edc.dsp)
    runtimeOnly(libs.edc.spi.jwt)
    runtimeOnly(libs.bundles.edc.dpf)

    runtimeOnly(libs.edc.ext.http)
    runtimeOnly(libs.bundles.edc.monitoring)
    runtimeOnly(libs.edc.controlplane.callback.dispatcher.event)
    runtimeOnly(libs.edc.controlplane.callback.dispatcher.http)

    // cloud provisioner extensions
    runtimeOnly(libs.edc.azure.blob.provision)

    // Federated Catalog Crawler + Query API
    runtimeOnly(project(":edc-extensions:federated-catalog"))
    runtimeOnly(libs.edc.fc.core)
    runtimeOnly(libs.edc.fc.api)


}
