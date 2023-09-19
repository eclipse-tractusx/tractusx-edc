/*
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
    runtimeOnly(project(":core:edr-cache-core"))
    runtimeOnly(project(":edc-extensions:dataplane-proxy:edc-dataplane-proxy-consumer-api"))
    runtimeOnly(project(":edc-extensions:dataplane-proxy:edc-dataplane-proxy-provider-api"))
    runtimeOnly(project(":edc-extensions:dataplane-proxy:edc-dataplane-proxy-provider-core"))

    runtimeOnly(libs.edc.config.filesystem)
    runtimeOnly(libs.edc.auth.tokenbased)
    runtimeOnly(libs.edc.dpf.awss3)
    runtimeOnly(libs.edc.dpf.azblob)
    runtimeOnly(libs.edc.dpf.oauth2)
    runtimeOnly(libs.edc.dpf.http)

    runtimeOnly(libs.edc.dpf.core)
    runtimeOnly(libs.edc.controlplane.apiclient)

    runtimeOnly(libs.edc.dpf.api)
    runtimeOnly(libs.edc.core.connector)
    runtimeOnly(libs.edc.boot)

    runtimeOnly(libs.bundles.edc.monitoring)
    runtimeOnly(libs.edc.ext.http)
    runtimeOnly(libs.edc.api.observability)
}
