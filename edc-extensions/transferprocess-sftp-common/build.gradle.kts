plugins {
    `maven-publish`
    `java-library`
}

dependencies {
    implementation(libs.edc.spi.core)
    testImplementation(libs.edc.junit)

    testImplementation(libs.mockito.inline)
    testImplementation(libs.testcontainers.junit)
}
