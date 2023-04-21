plugins {
    `maven-publish`
    `java-library`
}

dependencies {
    implementation(project(":edc-extensions:transferprocess-sftp-common"))
    implementation(libs.edc.spi.core)
    implementation(libs.edc.spi.transfer)
    implementation(libs.edc.spi.policy)
    implementation(libs.edc.spi.dataplane.dataplane)
    implementation(libs.edc.dpf.util)
    implementation(libs.edc.dpf.core)
    implementation(libs.edc.policy.engine)
    implementation(libs.bouncyCastle.bcpkixJdk18on)

    implementation(libs.apache.sshd.core)
    implementation(libs.apache.sshd.sftp)

    testImplementation(libs.awaitility)
    testImplementation(libs.edc.junit)

    testImplementation(libs.mockito.inline)
    testImplementation(libs.testcontainers.junit)
}
