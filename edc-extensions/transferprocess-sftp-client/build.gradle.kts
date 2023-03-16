
plugins {
    `maven-publish`
    `java-library`
}

dependencies {
    implementation(project(":edc-extensions:transferprocess-sftp-common"))
    implementation(edc.spi.core)
    implementation(edc.spi.transfer)
    implementation(edc.spi.policy)
    implementation(edc.spi.dataplane.dataplane)
    implementation(edc.dpf.util)
    implementation(edc.dpf.core)
    implementation(edc.policy.engine)
    implementation(libs.bouncyCastle.bcpkix)

    implementation(libs.apache.sshd.core)
    implementation(libs.apache.sshd.sftp)

    testImplementation(libs.awaitility)
    testImplementation(edc.junit)

    testImplementation(libs.mockito.inline)
    testImplementation(libs.testcontainers.junit)
}
