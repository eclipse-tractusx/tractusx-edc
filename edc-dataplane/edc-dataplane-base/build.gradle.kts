/********************************************************************************
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
    runtimeOnly(libs.edc.bom.dataplane.base)

    runtimeOnly(project(":core:edr-core"))
    runtimeOnly(project(":edc-extensions:log4j2-monitor"))
    runtimeOnly(project(":edc-extensions:dataplane:dataplane-proxy:dataplane-proxy-http"))
    runtimeOnly(project(":edc-extensions:dataplane:dataplane-proxy:dataplane-public-api-v2"))
    runtimeOnly(project(":edc-extensions:dataplane:dataplane-proxy:edc-dataplane-proxy-consumer-api"))
    runtimeOnly(project(":edc-extensions:dataplane:dataplane-token-refresh:token-refresh-api"))
    runtimeOnly(project(":edc-extensions:dataplane:dataplane-token-refresh:token-refresh-core"))
    runtimeOnly(project(":edc-extensions:dcp:tx-dcp-sts-dim"))
    runtimeOnly(project(":edc-extensions:tokenrefresh-handler"))
    runtimeOnly(project(":edc-extensions:event-subscriber"))
    runtimeOnly(project(":edc-extensions:non-finite-provider-push:non-finite-provider-push-core"))

    runtimeOnly(project(":edc-extensions:dataplane:dataflow:dataflow-api"))
    runtimeOnly(project(":edc-extensions:dataplane:dataflow:dataflow-service"))
    runtimeOnly(libs.edc.api.management.config)
    runtimeOnly(libs.edc.auth.tokenbased)
    runtimeOnly(libs.edc.auth.configuration)
    runtimeOnly(libs.edc.auth.delegated)

    runtimeOnly(libs.bundles.edc.monitoring)
    runtimeOnly(libs.edc.aws.validator.data.address.s3)
    runtimeOnly(libs.edc.core.did) // for the DID Public Key Resolver
    runtimeOnly(libs.edc.core.edrstore)
    runtimeOnly(libs.edc.dpf.awss3)
    runtimeOnly(libs.edc.dpf.azblob)
    runtimeOnly(libs.edc.identity.did.web)
    runtimeOnly(libs.log4j2.core)
}
