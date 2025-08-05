/**
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
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

rootProject.name = "tractusx-edc"

// this is needed to have access to snapshot builds of plugins
pluginManagement {
    repositories {
        maven {
            url = uri("https://central.sonatype.com/repository/maven-snapshots/")
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

// spi modules
include(":spi:callback-spi")
include(":spi:edr-spi")
include(":spi:core-spi")
include(":spi:tokenrefresh-spi")
include(":spi:bdrs-client-spi")
include(":spi:dataflow-spi")


// core modules
include(":core:edr-core")
include(":core:json-ld-core")
include(":core:core-utils")

// extensions - control plane
include(":edc-extensions:bpn-validation")
include(":edc-extensions:bpn-validation:bpn-validation-api")
include(":edc-extensions:bpn-validation:bpn-validation-spi")
include(":edc-extensions:bpn-validation:bpn-validation-core")
include(":edc-extensions:bpn-validation:business-partner-store-sql")
include(":edc-extensions:migrations:postgresql-migration-lib")
include(":edc-extensions:migrations:control-plane-migration")
include(":edc-extensions:migrations:data-plane-migration")
include(":edc-extensions:tokenrefresh-handler")
include(":edc-extensions:bdrs-client")
include(":edc-extensions:provision-additional-headers")
include(":edc-extensions:federated-catalog")
include(":edc-extensions:event-subscriber")
include(":edc-extensions:edr:edr-api-v2")
include(":edc-extensions:edr:edr-callback")
include(":edc-extensions:edr:edr-index-lock-sql")
include(":edc-extensions:cx-policy")
include(":edc-extensions:dcp:tx-dcp")
include(":edc-extensions:dcp:tx-dcp-sts-dim")
include(":edc-extensions:data-flow-properties-provider")
include(":edc-extensions:validators:empty-asset-selector")
include(":edc-extensions:log4j2-monitor")
include("edc-extensions:connector-discovery")

include(":edc-extensions:agreements")
include(":edc-extensions:agreements:retirement-evaluation-core")
include(":edc-extensions:agreements:retirement-evaluation-api")
include(":edc-extensions:agreements:retirement-evaluation-spi")
include(":edc-extensions:agreements:retirement-evaluation-store-sql")

// extensions - data plane
include(":edc-extensions:dataplane:dataplane-proxy:edc-dataplane-proxy-consumer-api")
include(":edc-extensions:dataplane:dataplane-util")
include(":edc-extensions:dataplane:dataplane-proxy:dataplane-proxy-http")
include(":edc-extensions:dataplane:dataplane-selector-configuration")
include(":edc-extensions:dataplane:dataplane-token-refresh:token-refresh-core")
include(":edc-extensions:dataplane:dataplane-token-refresh:token-refresh-api")
include(":edc-extensions:dataplane:dataplane-proxy:dataplane-public-api-v2")
include(":edc-extensions:dataplane:dataflow:dataflow-api")
include(":edc-extensions:dataplane:dataflow:dataflow-service")

include(":edc-extensions:non-finite-provider-push:non-finite-provider-push-spi")
include(":edc-extensions:non-finite-provider-push:non-finite-provider-push-core")

// test modules
include(":edc-tests:e2e-fixtures")
include(":edc-tests:e2e:bpn-event-tests")
include(":edc-tests:e2e:catalog-tests")
include(":edc-tests:e2e:cloud-transfer-tests")
include(":edc-tests:e2e:edc-dataplane-tokenrefresh-tests")
include(":edc-tests:e2e:edr-api-tests")
include(":edc-tests:e2e:end2end-transfer-cloud")
include(":edc-tests:e2e:management-tests")
include(":edc-tests:e2e:iatp-tests")
include(":edc-tests:e2e:policy-tests")
include(":edc-tests:e2e:transfer-tests")
include("edc-tests:e2e:discovery-tests")
include(":edc-tests:e2e:dcp-tck-tests")
include(":edc-tests:runtime:dataplane-cloud")
include(":edc-tests:runtime:iatp:iatp-extensions")
include(":edc-tests:runtime:iatp:runtime-memory-iatp-dim-ih")
include(":edc-tests:runtime:iatp:runtime-memory-iatp-ih")
include(":edc-tests:runtime:iatp:runtime-memory-sts")
include(":edc-tests:runtime:mock-connector")
include(":edc-tests:runtime:runtime-postgresql")
include(":edc-tests:runtime:runtime-dsp")
include("edc-tests:runtime:runtime-discovery:runtime-discovery-base")
include("edc-tests:runtime:runtime-discovery:runtime-discovery-no-protocols")
include("edc-tests:runtime:runtime-discovery:runtime-discovery-with-dsp-v08")
include(":edc-tests:dsp-compatibility-tests:compatibility-test-runner")

// modules for controlplane artifacts
include(":edc-controlplane")
include(":edc-controlplane:edc-controlplane-base")
include(":edc-controlplane:edc-runtime-memory")
include(":edc-controlplane:edc-controlplane-postgresql-hashicorp-vault")

// modules for dataplane artifacts
include(":edc-dataplane")
include(":edc-dataplane:edc-dataplane-base")
include(":edc-dataplane:edc-dataplane-hashicorp-vault")

include(":samples:testing-with-mocked-connector")

plugins {
    id("com.gradle.develocity") version "4.1"
    id("com.gradle.common-custom-user-data-gradle-plugin") version "2.3"
}

// Develocity
val isCI = System.getenv("CI") != null // adjust to your CI provider

develocity {
    server = "https://develocity-staging.eclipse.org"
    projectId = "automotive.tractusx"
    buildScan {
        uploadInBackground = !isCI
        publishing.onlyIf { it.isAuthenticated }
        obfuscation {
            ipAddresses { addresses -> addresses.map { _ -> "0.0.0.0" } }
        }
    }
}

buildCache {
    local {
        isEnabled = true
    }

    remote(develocity.buildCache) {
        isEnabled = true
        isPush = isCI
    }
}
