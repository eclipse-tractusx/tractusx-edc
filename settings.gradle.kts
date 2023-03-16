rootProject.name = "product-edc"

include(":edc-extensions:business-partner-validation")
include(":edc-extensions:control-plane-adapter")
include(":edc-extensions:cx-oauth2")
include(":edc-extensions:data-encryption")
include(":edc-extensions:dataplane-selector-configuration")
include(":edc-extensions:hashicorp-vault")
include(":edc-extensions:postgresql-migration")
include(":edc-extensions:provision-additional-headers")
include(":edc-extensions:transferprocess-sftp-client")
include(":edc-extensions:transferprocess-sftp-common")
include(":edc-extensions:transferprocess-sftp-provisioner")
include(":edc-tests")

// modules for controlplane artifacts
include(":edc-controlplane")
include(":edc-controlplane:edc-controlplane-base")
include(":edc-controlplane:edc-controlplane-memory")
include(":edc-controlplane:edc-controlplane-memory-hashicorp-vault")
include(":edc-controlplane:edc-controlplane-postgresql")
include(":edc-controlplane:edc-controlplane-postgresql-hashicorp-vault")

// modules for dataplane artifacts
include(":edc-dataplane")
include(":edc-dataplane:edc-dataplane-azure-vault")
include(":edc-dataplane:edc-dataplane-base")
include(":edc-dataplane:edc-dataplane-hashicorp-vault")

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
    versionCatalogs {
        create("libs") {
            from("org.eclipse.edc:edc-versions:0.0.1-20230220-SNAPSHOT")
            library("testcontainers-junit", "org.testcontainers","junit-jupiter").version("1.17.6")
            library("apache-sshd-core", "org.apache.sshd","sshd-core").version("2.9.2")
            library("apache-sshd-sftp", "org.apache.sshd","sshd-sftp").version("2.9.2")
        }
        // create version catalog for all EDC modules
        create("edc") {
            version("edc", "0.0.1-20230220-SNAPSHOT")
            library("spi-catalog", "org.eclipse.edc", "catalog-spi").versionRef("edc")
            library("spi-transfer", "org.eclipse.edc", "transfer-spi").versionRef("edc")
            library("spi-core", "org.eclipse.edc", "core-spi").versionRef("edc")
            library("spi-policy", "org.eclipse.edc", "policy-spi").versionRef("edc")
            library("spi-policyengine", "org.eclipse.edc", "policy-engine-spi").versionRef("edc")
            library("spi-transaction-datasource", "org.eclipse.edc", "transaction-datasource-spi").versionRef("edc")
            library("spi-transactionspi", "org.eclipse.edc", "transaction-spi").versionRef("edc")
            library("spi-aggregateservices", "org.eclipse.edc", "aggregate-service-spi").versionRef("edc")
            library("spi-web", "org.eclipse.edc", "web-spi").versionRef("edc")
            library("spi-jwt", "org.eclipse.edc", "jwt-spi").versionRef("edc")
            library("spi-oauth2", "org.eclipse.edc", "oauth2-spi").versionRef("edc")
            library("util", "org.eclipse.edc", "util").versionRef("edc")
            library("boot", "org.eclipse.edc", "boot").versionRef("edc")
            library("config-filesystem", "org.eclipse.edc", "configuration-filesystem").versionRef("edc")
            library("core-controlplane", "org.eclipse.edc", "control-plane-core").versionRef("edc")
            library("core-connector", "org.eclipse.edc", "connector-core").versionRef("edc")
            library("core-jetty", "org.eclipse.edc", "jetty-core").versionRef("edc")
            library("core-jersey", "org.eclipse.edc", "jersey-core").versionRef("edc")
            library("junit", "org.eclipse.edc", "junit").versionRef("edc")
            library("api-management-config", "org.eclipse.edc", "management-api-configuration").versionRef("edc")
            library("api-management", "org.eclipse.edc", "management-api").versionRef("edc")
            library("api-observability", "org.eclipse.edc", "api-observability").versionRef("edc")
            library("ext-http", "org.eclipse.edc", "http").versionRef("edc")
            library("spi-ids", "org.eclipse.edc", "ids-spi").versionRef("edc")
            library("ids", "org.eclipse.edc", "ids").versionRef("edc")
            library("iam-mock", "org.eclipse.edc", "iam-mock").versionRef("edc")
            library("ext-azure-cosmos-core", "org.eclipse.edc", "azure-cosmos-core").versionRef("edc")
            library("ext-azure-test", "org.eclipse.edc", "azure-test").versionRef("edc")
            library("policy-engine", "org.eclipse.edc", "policy-engine").versionRef("edc")
            library("auth-tokenbased", "org.eclipse.edc", "auth-tokenbased").versionRef("edc")
            library("auth-oauth2-core", "org.eclipse.edc", "oauth2-core").versionRef("edc")
            library("auth-oauth2-daps", "org.eclipse.edc", "oauth2-daps").versionRef("edc")
            library("transaction-local", "org.eclipse.edc", "transaction-local").versionRef("edc")

            // implementations
            library("sql-assetindex", "org.eclipse.edc", "asset-index-sql").versionRef("edc")
            library("sql-contract-definition", "org.eclipse.edc", "contract-definition-store-sql").versionRef("edc")
            library("sql-contract-negotiation", "org.eclipse.edc", "contract-negotiation-store-sql").versionRef("edc")
            library("sql-transferprocess", "org.eclipse.edc", "transfer-process-store-sql").versionRef("edc")
            library("sql-policydef", "org.eclipse.edc", "policy-definition-store-sql").versionRef("edc")
            library("sql-core", "org.eclipse.edc", "sql-core").versionRef("edc")
            library("sql-lease", "org.eclipse.edc", "sql-lease").versionRef("edc")
            library("sql-pool", "org.eclipse.edc", "sql-pool-apache-commons").versionRef("edc")

            // azure stuff
            library("azure-vault", "org.eclipse.edc", "vault-azure").versionRef("edc")
            library("azure-identity", "com.azure:azure-identity:+")

            // DPF modules
            library("spi-dataplane-dataplane", "org.eclipse.edc", "data-plane-spi").versionRef("edc")
            library("spi-dataplane-transfer", "org.eclipse.edc", "transfer-data-plane-spi").versionRef("edc")
            library("spi-dataplane-selector", "org.eclipse.edc", "data-plane-selector-spi").versionRef("edc")
            library("dpf-transferclient", "org.eclipse.edc", "data-plane-transfer-client").versionRef("edc")
            library("dpf-selector-client", "org.eclipse.edc", "data-plane-selector-client").versionRef("edc")
            library("dpf-selector-spi", "org.eclipse.edc", "data-plane-selector-spi").versionRef("edc")
            library("dpf-selector-core", "org.eclipse.edc", "data-plane-selector-core").versionRef("edc")
            library("dpf-transfer", "org.eclipse.edc", "transfer-data-plane").versionRef("edc")
            library("dpf-framework", "org.eclipse.edc", "data-plane-framework").versionRef("edc")
            library("dpf-core", "org.eclipse.edc", "data-plane-core").versionRef("edc")
            library("dpf-util", "org.eclipse.edc", "data-plane-util").versionRef("edc")
            library("dpf-awss3", "org.eclipse.edc", "data-plane-aws-s3").versionRef("edc")
            library("dpf-http", "org.eclipse.edc", "data-plane-http").versionRef("edc")
            library("dpf-oauth2", "org.eclipse.edc", "data-plane-http-oauth2").versionRef("edc")
            library("dpf-api", "org.eclipse.edc", "data-plane-api").versionRef("edc")

            // micrometer and other infra stuff
            library("micrometer-core", "org.eclipse.edc", "micrometer-core").versionRef("edc")
            library("micrometer-jersey", "org.eclipse.edc", "jersey-micrometer").versionRef("edc")
            library("micrometer-jetty", "org.eclipse.edc", "jetty-micrometer").versionRef("edc")
            library("monitor-jdklogger", "org.eclipse.edc", "monitor-jdk-logger").versionRef("edc")
            library(
                "transfer.dynamicreceiver",
                "org.eclipse.edc",
                "transfer-pull-http-dynamic-receiver"
            ).versionRef("edc")

            bundle(
                "connector",
                listOf("boot", "core-connector", "core-jersey", "core-controlplane", "api-observability")
            )

            bundle(
                "dpf",
                listOf("dpf-transfer", "dpf-selector-core", "dpf-selector-client", "spi-dataplane-selector")
            )

            bundle(
                "sqlstores",
                listOf(
                    "sql-assetindex",
                    "sql-contract-definition",
                    "sql-contract-negotiation",
                    "sql-transferprocess",
                    "sql-policydef"
                )
            )

            bundle(
                "monitoring",
                listOf("micrometer-core", "micrometer-jersey", "micrometer-jetty")
//                listOf("micrometer-core", "micrometer-jersey", "micrometer-jetty", "monitor-jdklogger")
            )
        }
    }
}
