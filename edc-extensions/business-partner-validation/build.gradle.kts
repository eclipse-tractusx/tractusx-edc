plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(libs.edc.spi.core)
    implementation(libs.edc.spi.policy)
    implementation(libs.edc.spi.contract)
    implementation(libs.edc.spi.policyengine)
}
