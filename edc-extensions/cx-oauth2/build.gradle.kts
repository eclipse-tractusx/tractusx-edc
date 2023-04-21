plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    implementation(libs.edc.spi.core)
    implementation(libs.edc.spi.oauth2)
    implementation(libs.edc.spi.jwt)
    implementation(libs.slf4j.api)
    implementation(libs.nimbus.jwt)
    implementation(libs.okhttp)
}
