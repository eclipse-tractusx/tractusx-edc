rootProject.name = "tractusx-edc"


include(":edc-extensions:business-partner-validation")
include(":edc-extensions:control-plane-adapter")
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



include(":edc-tests:e2e-tests")
include(":edc-tests:runtime")
include(":edc-tests:cucumber")

// modules for controlplane artifacts
include(":edc-controlplane")
include(":edc-controlplane:edc-controlplane-base")
include(":edc-controlplane:edc-runtime-memory")
include(":edc-controlplane:edc-controlplane-memory-hashicorp-vault")
include(":edc-controlplane:edc-controlplane-postgresql")
include(":edc-controlplane:edc-controlplane-postgresql-hashicorp-vault")

// modules for dataplane artifacts
include(":edc-dataplane")
include(":edc-dataplane:edc-dataplane-azure-vault")
include(":edc-dataplane:edc-dataplane-base")
include(":edc-dataplane:edc-dataplane-hashicorp-vault")

// Version Catalog
include(":version-catalog")

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
