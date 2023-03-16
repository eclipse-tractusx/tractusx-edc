
plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    implementation(edc.spi.core)
    implementation(edc.spi.oauth2)
    implementation(edc.spi.jwt)
    implementation(libs.slf4j.api)
    implementation(libs.nimbus.jwt)
    implementation(libs.okhttp)
}
