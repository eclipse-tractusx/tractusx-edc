
plugins {
    `maven-publish`
    `java-library`
}

dependencies {
    implementation(project(":edc-extensions:transferprocess-sftp-common"))

    implementation(edc.spi.core)
    implementation(edc.policy.engine)
    implementation(edc.spi.transfer)

    testImplementation(edc.junit)
    testImplementation(libs.mockito.inline)
    testImplementation(libs.testcontainers.junit)
}
