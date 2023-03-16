
plugins {
    `maven-publish`
    `java-library`
}

dependencies {
    implementation(edc.spi.core)
    implementation(edc.junit)
    implementation(libs.bouncyCastle.bcpkix)
    implementation(libs.okhttp)
    implementation("org.testcontainers:junit-jupiter:1.17.6")
    implementation("org.testcontainers:vault:1.17.6")
    testImplementation(libs.mockito.inline)
}
