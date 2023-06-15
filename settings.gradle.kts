/**
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
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

rootProject.name = "tractusx-edc"

// spi modules
include(":spi:control-plane-adapter-spi")
include(":spi:edr-cache-spi")
include(":spi:core-spi")
include(":spi:ssi-spi")


// core modules
include(":core:edr-cache-core")


include(":edc-extensions:business-partner-validation")
include(":edc-extensions:cx-oauth2")
include(":edc-extensions:data-encryption")
include(":edc-extensions:dataplane-selector-configuration")
include(":edc-extensions:hashicorp-vault")
include(":edc-extensions:postgresql-migration")
include(":edc-extensions:provision-additional-headers")
include(":edc-extensions:observability-api-customization")
include(":edc-extensions:transferprocess-sftp-client")
include(":edc-extensions:transferprocess-sftp-common")
include(":edc-extensions:transferprocess-sftp-provisioner")
include(":edc-extensions:control-plane-adapter-api")
include(":edc-extensions:control-plane-adapter-callback")
include(":edc-extensions:edr-cache-sql")
include("edc-extensions:ssi:ssi-identity-core")
include("edc-extensions:ssi:ssi-miw-credential-client")



include(":edc-tests:e2e-tests")
include(":edc-tests:runtime:extensions")
include(":edc-tests:runtime:runtime-memory")
include(":edc-tests:runtime:runtime-memory-ssi")
include(":edc-tests:runtime:runtime-postgresql")
include(":edc-tests:cucumber")

// modules for controlplane artifacts
include(":edc-controlplane")
include(":edc-controlplane:edc-controlplane-base")
include(":edc-controlplane:edc-runtime-memory")
include(":edc-controlplane:edc-controlplane-memory-hashicorp-vault")
include(":edc-controlplane:edc-controlplane-postgresql-azure-vault")
include(":edc-controlplane:edc-controlplane-postgresql-hashicorp-vault")

// modules for dataplane artifacts
include(":edc-dataplane")
include(":edc-dataplane:edc-dataplane-azure-vault")
include(":edc-dataplane:edc-dataplane-base")
include(":edc-dataplane:edc-dataplane-hashicorp-vault")
include(":edc-dataplane:edc-dataplane-proxy-consumer-api")
include(":edc-dataplane:edc-dataplane-proxy-provider-spi")
include(":edc-dataplane:edc-dataplane-proxy-provider-core")
include(":edc-dataplane:edc-dataplane-proxy-provider-api")
include(":edc-tests:edc-dataplane-proxy-e2e")


// this is needed to have access to snapshot builds of plugins
pluginManagement {
    repositories {
        maven {
            url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        maven {
            url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
        }
        mavenCentral()
        mavenLocal()
    }
}
