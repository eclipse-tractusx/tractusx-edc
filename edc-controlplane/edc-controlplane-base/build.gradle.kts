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
}

dependencies {
    runtimeOnly(project(":core:edr-cache-core"))
    runtimeOnly(project(":core:edr-core"))
    runtimeOnly(project(":edc-extensions:dataplane-selector-configuration"))
    runtimeOnly(project(":edc-extensions:data-encryption"))

    runtimeOnly(project(":edc-extensions:provision-additional-headers"))
    runtimeOnly(project(":edc-extensions:edr:edr-api"))
    runtimeOnly(project(":edc-extensions:edr:edr-callback"))

    // needed for BPN validation
    runtimeOnly(project(":edc-extensions:bpn-validation"))

    // needed for SSI integration
    runtimeOnly(project(":core:json-ld-core"))
    runtimeOnly(project(":edc-extensions:ssi:ssi-identity-core"))
    runtimeOnly(project(":edc-extensions:ssi:ssi-miw-credential-client"))
    runtimeOnly(project(":edc-extensions:ssi:ssi-identity-extractor"))
    runtimeOnly(project(":edc-extensions:cx-policy"))

    runtimeOnly(libs.edc.core.controlplane)
    runtimeOnly(libs.edc.core.policy.monitor)
    runtimeOnly(libs.edc.config.filesystem)
    runtimeOnly(libs.edc.auth.tokenbased)
    runtimeOnly(libs.edc.validator.data.address.http.data)

    runtimeOnly(libs.edc.api.management)
    runtimeOnly(libs.edc.api.controlplane)
    runtimeOnly(libs.edc.api.management.config)
    runtimeOnly(libs.edc.api.observability)
    runtimeOnly(libs.edc.dsp)
    runtimeOnly(libs.edc.spi.jwt)
    runtimeOnly(libs.bundles.edc.dpf)

    runtimeOnly(libs.edc.ext.http)
    runtimeOnly(libs.bundles.edc.monitoring)
    runtimeOnly(libs.edc.transfer.dynamicreceiver)
    runtimeOnly(libs.edc.controlplane.callback.dispatcher.event)
    runtimeOnly(libs.edc.controlplane.callback.dispatcher.http)

}
