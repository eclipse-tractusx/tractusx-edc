plugins {
    `java-library`
}

dependencies {
    testImplementation(libs.restAssured)
    testImplementation(libs.postgres)
    testImplementation(libs.awaitility)
    testImplementation(libs.aws.s3)
    testImplementation(edc.spi.core)
    testImplementation(edc.junit)
    testImplementation(edc.spi.policy)
    testImplementation(edc.spi.contract)
    testImplementation(edc.core.api)
    testImplementation(edc.spi.catalog)
    testImplementation(edc.api.catalog)
    testImplementation(testFixtures(edc.junit))
}
