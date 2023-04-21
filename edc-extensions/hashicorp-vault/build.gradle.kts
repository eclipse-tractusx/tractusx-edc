plugins {
    `maven-publish`
    `java-library`
}

dependencies {
    implementation(libs.edc.spi.core)
    implementation(libs.edc.junit)
    implementation(libs.bouncyCastle.bcpkixJdk18on)
    implementation(libs.okhttp)
    implementation("org.testcontainers:vault:1.18.0")
    implementation("org.testcontainers:junit-jupiter:1.18.0")
    testImplementation(libs.mockito.inline)
}
