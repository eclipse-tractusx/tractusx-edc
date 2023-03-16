
plugins {
    `maven-publish`
    `java-library`
}

dependencies {
    implementation(edc.spi.core)
    implementation(edc.junit)
    implementation(edc.spi.transaction.datasource)
    implementation(edc.sql.assetindex)
    implementation(edc.sql.core)

    implementation("org.flywaydb:flyway-core:9.15.2")
}
