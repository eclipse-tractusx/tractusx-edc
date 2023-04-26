plugins {
    `maven-publish`
    `java-library`
}

dependencies {
    implementation(libs.edc.spi.core)
    implementation(libs.edc.junit)
    implementation(libs.edc.spi.transaction.datasource)
    implementation(libs.edc.sql.assetindex)
    implementation(libs.edc.sql.core)

    implementation("org.flywaydb:flyway-core:9.16.3")
}
