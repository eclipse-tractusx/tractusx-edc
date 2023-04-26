plugins {
    `maven-publish`
    `java-library`
}

dependencies {
    implementation(project(":edc-extensions:transferprocess-sftp-common"))

    implementation(libs.edc.spi.core)
    implementation(libs.edc.policy.engine)
    implementation(libs.edc.spi.transfer)

    testImplementation(libs.edc.junit)
    testImplementation(libs.mockito.inline)
    testImplementation(libs.testcontainers.junit)
}
