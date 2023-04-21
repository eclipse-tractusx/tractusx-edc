plugins {
    `maven-publish`
    `java-library`
}

dependencies {
    implementation(libs.edc.spi.core)
    implementation(libs.edc.spi.transfer)

    testImplementation(libs.awaitility)
    testImplementation(libs.edc.junit)

    testImplementation(libs.edc.core.controlplane)
    testImplementation(libs.edc.dpf.selector.core)
    testImplementation(libs.edc.ids)
    testImplementation(libs.edc.iam.mock)
    testImplementation(libs.mockito.inline)
}
