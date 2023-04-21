
plugins {
    `java-library`
    `maven-publish`
    id("io.swagger.core.v3.swagger-gradle-plugin")
}

dependencies {
    implementation(edc.spi.core)
    implementation(edc.spi.policy)

    implementation(edc.api.management)
    constraints {
        implementation("org.yaml:snakeyaml:2.0") {
            because("version 1.33 has vulnerabilities: https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2022-1471.")
        }
    }

    implementation(edc.spi.catalog)
    implementation(edc.spi.transactionspi)
    implementation(edc.spi.transaction.datasource)
    implementation(edc.ids)
    implementation(edc.sql.core)
    implementation(edc.sql.lease)
    implementation(edc.sql.pool)


    implementation(libs.postgres)
    implementation(libs.jakarta.rsApi)


    implementation(edc.spi.aggregateservices)
    testImplementation(libs.awaitility)
}
