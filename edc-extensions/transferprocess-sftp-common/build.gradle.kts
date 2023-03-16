
plugins {
    `maven-publish`
    `java-library`
}

dependencies {
    implementation(edc.spi.core)
    testImplementation(edc.junit)

    testImplementation(libs.mockito.inline)
    testImplementation(libs.testcontainers.junit)}
