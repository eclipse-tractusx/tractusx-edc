/*
 * Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
    `java-library`
}

dependencies {
    runtimeOnly(project(":edc-extensions:business-partner-validation"))
    runtimeOnly(project(":edc-extensions:dataplane-selector-configuration"))
    runtimeOnly(project(":edc-extensions:data-encryption"))
    runtimeOnly(project(":edc-extensions:cx-oauth2"))
    runtimeOnly(project(":edc-extensions:control-plane-adapter"))
    runtimeOnly(project(":edc-extensions:provision-additional-headers"))
    runtimeOnly(project(":edc-extensions:observability-api-customization"))

    runtimeOnly(edc.core.controlplane)
    runtimeOnly(edc.config.filesystem)
    runtimeOnly(edc.auth.tokenbased)
    runtimeOnly(edc.auth.oauth2.core)
    runtimeOnly(edc.auth.oauth2.daps)
    runtimeOnly(edc.api.management)
    runtimeOnly(edc.ids)
    runtimeOnly(edc.spi.jwt)
    runtimeOnly(edc.bundles.dpf)

    runtimeOnly(edc.ext.http)
    runtimeOnly(edc.bundles.monitoring)
    runtimeOnly(edc.transfer.dynamicreceiver)
}
