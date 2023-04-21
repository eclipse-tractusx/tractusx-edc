plugins {
    `java-library`
    `maven-publish`
    id("io.swagger.core.v3.swagger-gradle-plugin")
}

dependencies {
    implementation(libs.edc.spi.core)
    implementation(libs.edc.spi.policy)

    implementation(libs.edc.api.management)
    constraints {
        implementation("org.yaml:snakeyaml:2.0") {
            because("version 1.33 has vulnerabilities: https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2022-1471.")
        }
    }

    implementation(libs.edc.spi.catalog)
    implementation(libs.edc.spi.transactionspi)
    implementation(libs.edc.spi.transaction.datasource)
    implementation(libs.edc.ids)
    implementation(libs.edc.sql.core)
    implementation(libs.edc.sql.lease)
    implementation(libs.edc.sql.pool)


    implementation(libs.postgres)
    implementation(libs.jakarta.rsApi)


    implementation(libs.edc.spi.aggregateservices)
    testImplementation(libs.awaitility)
}
