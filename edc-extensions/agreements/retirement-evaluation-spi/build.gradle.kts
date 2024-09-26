plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    implementation(libs.edc.spi.core)

    testImplementation(libs.edc.junit)
}
