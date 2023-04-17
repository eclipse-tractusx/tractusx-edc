
plugins {
    `maven-publish`
    `java-library`
}

dependencies {
    implementation(edc.spi.core)
    implementation(edc.junit)
    implementation(libs.bouncyCastle.bcpkix)
    implementation(libs.okhttp)
    implementation("org.testcontainers:vault:1.18.0")
    implementation("org.testcontainers:junit-jupiter:1.18.0")
    testImplementation(libs.mockito.inline)
}
